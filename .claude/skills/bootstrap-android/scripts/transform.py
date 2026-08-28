#!/usr/bin/env python3
"""
transform.py — deterministic rename/parameterise pass for a repo freshly
created from 3sidedcube/sprintzero-template-android-client.

The modern replacement for scripts/build-new-project.rb. Run it from anywhere:

  python3 transform.py --repo /path/to/clone \
      --display-name "<Display Name>" \
      --package com.cube.<appname> \
      [--api-envs dev,staging,live] \
      [--staging-url https://api.staging.example.com] \
      [--live-url https://api.example.com] \
      [--dev-url https://api.dev.example.com]

What it does (the rename inventory from the phase-2 handover):
  1. Global token replaces across text files:
       com.cube.sprintzerotemplate -> <package>
       SprintZeroTemplate          -> <PascalName>   (covers App class, Theme.*)
       Sprint Zero Template        -> <display name> (covers rootProject.name, app_name)
       sprintzerotemplate (loose)  -> <package last segment> (fallback)
  2. Moves main/test/androidTest source trees to the new package path
     (git mv when possible, preserving history).
  3. Renames files containing the template token (SprintZeroTemplateApp.kt etc).
  4. API environments (--api-envs, default dev,staging,live — the template
     ships all three, so the default is a no-op): any non-empty subset is
     accepted; deselected environments are REMOVED — the api<Env> product
     flavor, the assemble<Env>APKS Bitrise workflow — and branch triggers
     left pointing at a removed workflow are retargeted to the nearest
     surviving one. A legacy add path covers older-era clones missing apiDev.
  5. Optionally swaps dev/staging/live API_URL placeholder values.
  5b. Firebase environments (--firebase-envs, default staging,live — the
     template's flavors, a no-op): any non-empty subset of dev,staging,live.
     'dev' ADDS a firebaseDev flavor plus a placeholder google-services.json
     source set; deselecting staging/live REMOVES the flavor and its source
     set, remapping bitrise.yml variant/task names to a surviving flavor.
  6. Deletes scripts/build-new-project.rb (no-op safeguard; gone from the template).
  7. Deletes .claude/skills/bootstrap-android — clients must not inherit the
     skill that creates clients (.claude is also excluded from the walk so the
     skill's own template tokens never trip the verifier).
  8. Verifies: zero surviving template tokens (any case), TOML/JSON/YAML parse.

Exits non-zero and prints a report if anything survives. Idempotent-ish: a
second run on an already-transformed repo is a no-op that still passes.

Stdlib only. No network. Never touches .git internals beyond `git mv`.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

OLD_PACKAGE = "com.cube.sprintzerotemplate"
OLD_PASCAL = "SprintZeroTemplate"
OLD_DISPLAY = "Sprint Zero Template"
OLD_TOKEN_LOOSE = "sprintzerotemplate"

TEXT_EXTS = {
    ".kt", ".kts", ".xml", ".toml", ".md", ".yml", ".yaml", ".rb",
    ".json", ".properties", ".pro", ".gradle", ".txt", ".gitignore",
}
SKIP_DIRS = {".git", ".gradle", "build", ".idea", "node_modules", ".claude"}

# Validation regexes — package per Android rules. PascalCase came from the
# original ruby script but now also allows digits (legal in Kotlin class
# names; "Test2" was rejected by the verbatim ruby regex).
PACKAGE_RE = re.compile(r"^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$")
PASCAL_RE = re.compile(r"^[A-Z][a-z0-9]*(?:[a-z0-9]+[A-Z][a-z0-9]*)*$")


def pascal_from_display(display: str) -> str:
    words = re.findall(r"[A-Za-z0-9]+", display)
    return "".join(w[:1].upper() + w[1:].lower() for w in words)


def is_text_file(path: Path) -> bool:
    if path.suffix.lower() in TEXT_EXTS or path.name in (".gitignore", "gradlew"):
        return True
    return False


def iter_files(repo: Path):
    for root, dirs, files in os.walk(repo):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for f in files:
            yield Path(root) / f


def run_git(repo: Path, *args) -> bool:
    try:
        subprocess.run(
            ["git", *args], cwd=repo, check=True,
            capture_output=True, text=True,
        )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def in_git_repo(repo: Path) -> bool:
    return (repo / ".git").exists()


def move_tree(repo: Path, old_dir: Path, new_dir: Path, report: dict):
    """Move old_dir -> new_dir, via git mv when possible (history)."""
    new_dir.parent.mkdir(parents=True, exist_ok=True)
    moved = False
    if in_git_repo(repo):
        moved = run_git(repo, "mv", str(old_dir.relative_to(repo)),
                        str(new_dir.relative_to(repo)))
    if not moved:
        shutil.move(str(old_dir), str(new_dir))
    report["moved"].append(f"{old_dir.relative_to(repo)} -> {new_dir.relative_to(repo)}")


def prune_empty_dirs(repo: Path):
    for root, dirs, files in os.walk(repo, topdown=False):
        p = Path(root)
        if p == repo or any(part in SKIP_DIRS for part in p.parts):
            continue
        if not any(p.iterdir()):
            p.rmdir()


def xml_escape_apostrophes(value: str) -> str:
    # Android string resources need apostrophes escaped when unquoted.
    return re.sub(r"(?<!\\)'", r"\\'", value)


def replace_in_file(path: Path, replacements, report: dict) -> None:
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return
    original = text
    for old, new in replacements:
        if old in text:
            report["replacements"][old] = (
                report["replacements"].get(old, 0) + text.count(old)
            )
            text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8")
        report["files_edited"].add(str(path))


def swap_api_url(path: Path, flavor_hint: str, new_url: str, report: dict) -> None:
    """Replace the quoted URL on API_URL lines in the block for flavor_hint.

    Heuristic: within app/build.gradle.kts, an API_URL buildConfigField line
    belongs to the nearest enclosing flavor block (create("apiStaging") etc).
    We track the most recent create("...") name seen above each line.
    """
    if not path.exists():
        return
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    current_flavor = ""
    changed = False
    flavor_re = re.compile(r'create\("([A-Za-z0-9]+)"\)')
    url_re = re.compile(r'("API_URL"\s*,\s*)"\\?"?(https?://[^"\\]*)\\?"?"')
    for i, line in enumerate(lines):
        m = flavor_re.search(line)
        if m:
            current_flavor = m.group(1).lower()
        if "API_URL" in line and flavor_hint in current_flavor:
            new_line = url_re.sub(
                lambda m2: m2.group(1) + '"\\"' + new_url + '\\""', line)
            if new_line != line:
                lines[i] = new_line
                changed = True
    if changed:
        path.write_text("".join(lines), encoding="utf-8")
        report["api_urls"].append({flavor_hint: new_url})
    else:
        report["warnings"].append(
            f"could not locate an API_URL line for '{flavor_hint}' in "
            f"{path.name} — set it manually"
        )


DEV_FLAVOR_BLOCK = (
    '\t\tcreate("apiDev") {\n'
    '\t\t\tdimension = "api"\n'
    '\t\t\tbuildConfigField("String", "API_URL", "\\"https://api.dev.goes.here.com\\"")\n'
    '\t\t}\n'
)

DEV_TRIGGER = "  - push_branch: develop\n    workflow: assembleDevAPKS"
STAGING_TRIGGER = "  - push_branch: develop\n    workflow: assembleStagingAPKS"

DEV_BITRISE_BLOCK = """  assembleDevAPKS:
    before_run:
      - assemble
    envs:
      - opts:
          is_expand: false
        DEBUG_VARIANT: firebaseStagingApiDevDebug
      - opts:
          is_expand: false
        RELEASE_VARIANT: firebaseStagingApiDevRelease
      - opts:
          is_expand: false
        GRADLE_TASK: |-
          assembleFirebaseStagingApiDevRelease
          bundleFirebaseStagingApiDevRelease
"""


def add_dev_api_env(repo: Path, report: dict) -> None:
    """Add the apiDev product flavor and its Bitrise workflow. Idempotent."""
    gradle = repo / "app" / "build.gradle.kts"
    if gradle.exists():
        text = gradle.read_text(encoding="utf-8")
        anchor = '\t\tcreate("apiStaging") {'
        if 'create("apiDev")' in text:
            pass  # already present — second run is a no-op
        elif anchor in text:
            gradle.write_text(text.replace(anchor, DEV_FLAVOR_BLOCK + anchor, 1),
                              encoding="utf-8")
            report["api_envs"].append("apiDev flavor added to app/build.gradle.kts")
        else:
            report["warnings"].append(
                "could not locate the apiStaging flavor block to anchor apiDev "
                "— add the flavor manually")
    bitrise = repo / "bitrise.yml"
    if bitrise.exists():
        text = bitrise.read_text(encoding="utf-8")
        if "\n  assembleDevAPKS:" in text:
            pass  # workflow definition already present
        elif "\nmeta:" in text:
            text = text.replace("\nmeta:", "\n" + DEV_BITRISE_BLOCK + "meta:", 1)
            bitrise.write_text(text, encoding="utf-8")
            report["api_envs"].append("assembleDevAPKS workflow added to bitrise.yml")
        else:
            report["warnings"].append(
                "could not locate the meta: section in bitrise.yml to anchor "
                "assembleDevAPKS — add the workflow manually")
        # develop pushes build the dev variant when a dev env exists.
        if DEV_TRIGGER not in text and STAGING_TRIGGER in text:
            bitrise.write_text(
                text.replace(STAGING_TRIGGER, DEV_TRIGGER, 1), encoding="utf-8")
            report["api_envs"].append(
                "develop trigger retargeted to assembleDevAPKS in bitrise.yml")


ENV_FLAVORS = {"dev": "apiDev", "staging": "apiStaging", "live": "apiLive"}
ENV_WORKFLOWS = {"dev": "assembleDevAPKS", "staging": "assembleStagingAPKS",
                 "live": "assembleProdAPKS"}


def remove_api_env(repo: Path, env: str, report: dict) -> None:
    """Remove a deselected API environment: its product flavor block and its
    Bitrise workflow. Idempotent — absent blocks are fine on older-era clones."""
    flavor = ENV_FLAVORS[env]
    gradle = repo / "app" / "build.gradle.kts"
    if gradle.exists():
        text = gradle.read_text(encoding="utf-8")
        block_re = re.compile(
            r'\t\tcreate\("' + flavor + r'"\) \{\n(?:\t\t\t.*\n)*?\t\t\}\n')
        new = block_re.sub("", text, count=1)
        if new != text:
            gradle.write_text(new, encoding="utf-8")
            report["api_envs"].append(f"{flavor} flavor removed from app/build.gradle.kts")
        else:
            report["warnings"].append(
                f"could not locate the {flavor} flavor block to remove — "
                f"remove it manually if present")
    bitrise = repo / "bitrise.yml"
    if bitrise.exists():
        text = bitrise.read_text(encoding="utf-8")
        wf = ENV_WORKFLOWS[env]
        wf_re = re.compile(r"(?m)^  " + wf + r":\n(?:(?:    .*)?\n)*")
        new = wf_re.sub("", text, count=1)
        if new != text:
            bitrise.write_text(new, encoding="utf-8")
            report["api_envs"].append(f"{wf} workflow removed from bitrise.yml")


FIREBASE_FLAVORS = {"dev": "firebaseDev", "staging": "firebaseStaging",
                    "live": "firebaseLive"}

FIREBASE_DEV_BLOCK = (
    '\t\tcreate("firebaseDev") {\n'
    '\t\t\tdimension = "firebase"\n'
    '\t\t}\n'
)


def add_firebase_dev(repo: Path, report: dict) -> None:
    """Add the firebaseDev flavor and seed its placeholder google-services.json
    from an existing flavor's placeholder. Idempotent."""
    gradle = repo / "app" / "build.gradle.kts"
    if gradle.exists():
        text = gradle.read_text(encoding="utf-8")
        if 'create("firebaseDev")' not in text:
            for anchor_flavor in ("firebaseStaging", "firebaseLive"):
                anchor = f'\t\tcreate("{anchor_flavor}") {{'
                if anchor in text:
                    gradle.write_text(
                        text.replace(anchor, FIREBASE_DEV_BLOCK + anchor, 1),
                        encoding="utf-8")
                    report["firebase_envs"].append(
                        "firebaseDev flavor added to app/build.gradle.kts")
                    break
            else:
                report["warnings"].append(
                    "could not locate a firebase flavor block to anchor "
                    "firebaseDev — add the flavor manually")
    dev_config = repo / "app" / "src" / "firebaseDev" / "google-services.json"
    if not dev_config.exists():
        for src_flavor in ("firebaseStaging", "firebaseLive"):
            src = repo / "app" / "src" / src_flavor / "google-services.json"
            if src.exists():
                dev_config.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, dev_config)
                report["firebase_envs"].append(
                    "placeholder google-services.json seeded for firebaseDev")
                break
        else:
            report["warnings"].append(
                "no placeholder google-services.json found to seed firebaseDev "
                "— add one manually")
    report["warnings"].append(
        "firebaseDev variants are not covered by the committed lockfile — "
        "regenerate with ./gradlew :app:dependencies --write-locks")


def remove_firebase_env(repo: Path, env: str, report: dict) -> None:
    """Remove a deselected Firebase environment: flavor block + source set."""
    flavor = FIREBASE_FLAVORS[env]
    gradle = repo / "app" / "build.gradle.kts"
    if gradle.exists():
        text = gradle.read_text(encoding="utf-8")
        block_re = re.compile(
            r'\t\tcreate\("' + flavor + r'"\) \{\n(?:\t\t\t.*\n)*?\t\t\}\n')
        new = block_re.sub("", text, count=1)
        if new != text:
            gradle.write_text(new, encoding="utf-8")
            report["firebase_envs"].append(
                f"{flavor} flavor removed from app/build.gradle.kts")
        else:
            report["warnings"].append(
                f"could not locate the {flavor} flavor block to remove — "
                f"remove it manually if present")
    src_dir = repo / "app" / "src" / flavor
    if src_dir.exists():
        if not (in_git_repo(repo) and run_git(repo, "rm", "-r", "-q",
                                              str(src_dir.relative_to(repo)))):
            shutil.rmtree(src_dir)
        report["firebase_envs"].append(f"app/src/{flavor} source set removed")


def remap_firebase_in_bitrise(repo: Path, kept: set, report: dict) -> None:
    """bitrise.yml variant/task names embed firebase flavor names — remap any
    reference to a removed flavor onto the preferred surviving one."""
    bitrise = repo / "bitrise.yml"
    if not bitrise.exists():
        return
    preference = [e for e in ("staging", "live", "dev") if e in kept]
    if not preference:
        return
    target = FIREBASE_FLAVORS[preference[0]]
    text = bitrise.read_text(encoding="utf-8")
    changed = False
    for env, flavor in FIREBASE_FLAVORS.items():
        if env in kept:
            continue
        for needle, repl in ((flavor, target),
                             (flavor[0].upper() + flavor[1:],
                              target[0].upper() + target[1:])):
            if needle in text:
                text = text.replace(needle, repl)
                changed = True
        if changed:
            report["firebase_envs"].append(
                f"bitrise.yml references remapped {flavor} -> {target}")
    if changed:
        bitrise.write_text(text, encoding="utf-8")


def retarget_triggers(repo: Path, report: dict) -> None:
    """Point any branch trigger at a workflow that still exists. Preference
    order approximates the branch's intent: staging, then live, then dev."""
    bitrise = repo / "bitrise.yml"
    if not bitrise.exists():
        return
    text = bitrise.read_text(encoding="utf-8")
    defined = set(re.findall(r"(?m)^  (assemble\w+APKS):", text))
    preference = ["assembleStagingAPKS", "assembleProdAPKS", "assembleDevAPKS"]
    changed = False

    def fix(match: re.Match) -> str:
        nonlocal changed
        wf = match.group(1)
        if wf in defined:
            return match.group(0)
        for candidate in preference:
            if candidate in defined:
                changed = True
                report["api_envs"].append(f"bitrise trigger retargeted {wf} -> {candidate}")
                return match.group(0).replace(wf, candidate)
        return match.group(0)

    new = re.sub(r"(?m)^    workflow: (assemble\w+APKS)$", fix, text)
    if changed:
        bitrise.write_text(new, encoding="utf-8")


def verify(repo: Path, report: dict) -> bool:
    ok = True
    try:
        import tomllib  # Python 3.11+
    except ImportError:
        tomllib = None
        report["warnings"].append(
            "tomllib unavailable (Python < 3.11) — TOML parse checks skipped; "
            "rerun --verify-only with a newer python3 for full verification"
        )
    # 1. Zero surviving template tokens, any case, filenames included.
    token_re = re.compile(r"sprint\s*zero\s*template|sprintzerotemplate", re.IGNORECASE)
    for f in iter_files(repo):
        if token_re.search(f.name):
            report["survivors"].append(f"filename: {f.relative_to(repo)}")
            ok = False
        if not is_text_file(f):
            continue
        try:
            text = f.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for n, line in enumerate(text.splitlines(), 1):
            if token_re.search(line):
                report["survivors"].append(f"{f.relative_to(repo)}:{n}: {line.strip()[:120]}")
                ok = False
    # 2. Structured files still parse.
    for f in iter_files(repo):
        if f.suffix == ".json":
            try:
                json.loads(f.read_text(encoding="utf-8"))
            except Exception as e:
                report["parse_errors"].append(f"{f.relative_to(repo)}: {e}")
                ok = False
        elif f.suffix == ".toml":
            if tomllib is None:
                continue
            try:
                tomllib.loads(f.read_text(encoding="utf-8"))
            except Exception as e:
                report["parse_errors"].append(f"{f.relative_to(repo)}: {e}")
                ok = False
        elif f.suffix in (".yml", ".yaml"):
            try:
                import yaml  # optional; skip check if unavailable
                yaml.safe_load(f.read_text(encoding="utf-8"))
            except ImportError:
                pass
            except Exception as e:
                report["parse_errors"].append(f"{f.relative_to(repo)}: {e}")
                ok = False
    return ok


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", required=True, type=Path)
    ap.add_argument("--display-name", required=True)
    ap.add_argument("--package", required=True)
    ap.add_argument("--pascal-name", help="override derived PascalCase name")
    ap.add_argument("--staging-url")
    ap.add_argument("--live-url")
    ap.add_argument("--dev-url")
    ap.add_argument("--api-envs", default="dev,staging,live",
                    help="API environments to keep, any non-empty subset of "
                         "'dev,staging,live'. The template ships all three "
                         "(the default is a no-op); deselected environments "
                         "have their flavor and Bitrise workflow removed")
    ap.add_argument("--firebase-envs", default="staging,live",
                    help="Firebase environments to keep, any non-empty subset "
                         "of 'dev,staging,live'. The template ships staging + "
                         "live (the default is a no-op); 'dev' adds the "
                         "firebaseDev flavor, deselected ones are removed")
    ap.add_argument("--verify-only", action="store_true",
                    help="run only the verification pass")
    args = ap.parse_args()

    api_envs = sorted({e.strip().lower() for e in args.api_envs.split(",") if e.strip()})
    if not api_envs or not set(api_envs) <= set(ENV_FLAVORS):
        print("error: --api-envs must be a non-empty subset of 'dev,staging,live'",
              file=sys.stderr)
        return 2
    for env, url in (("dev", args.dev_url), ("staging", args.staging_url),
                     ("live", args.live_url)):
        if url and env not in api_envs:
            print(f"error: --{env}-url given but '{env}' is not in --api-envs",
                  file=sys.stderr)
            return 2
    firebase_envs = sorted({e.strip().lower()
                            for e in args.firebase_envs.split(",") if e.strip()})
    if not firebase_envs or not set(firebase_envs) <= set(FIREBASE_FLAVORS):
        print("error: --firebase-envs must be a non-empty subset of "
              "'dev,staging,live'", file=sys.stderr)
        return 2

    repo: Path = args.repo.resolve()
    if not repo.is_dir():
        print(f"error: {repo} is not a directory", file=sys.stderr)
        return 2

    package = args.package.strip()
    if not PACKAGE_RE.fullmatch(package):
        print(f"error: invalid package '{package}' — lowercase dot-separated "
              f"segments, each [a-z][a-z0-9]*", file=sys.stderr)
        return 2

    pascal = args.pascal_name or pascal_from_display(args.display_name)
    if not PASCAL_RE.fullmatch(pascal):
        print(f"error: derived class name '{pascal}' is not PascalCase — "
              f"pass --pascal-name explicitly", file=sys.stderr)
        return 2

    report = {
        "package": package, "pascal": pascal, "display": args.display_name,
        "replacements": {}, "files_edited": set(), "moved": [],
        "renamed": [], "deleted": [], "api_urls": [], "api_envs": [],
        "firebase_envs": [], "warnings": [], "survivors": [], "parse_errors": [],
    }

    if not args.verify_only:
        # -- 2. Move source trees first (paths change under us otherwise).
        pkg_path = Path(*package.split("."))
        old_pkg_path_str = str(Path(*OLD_PACKAGE.split(".")))
        old_trees = [
            Path(root) for root, dirs, _ in os.walk(repo)
            if root.endswith(old_pkg_path_str)
            and not any(part in SKIP_DIRS for part in Path(root).parts)
        ]
        for old_dir in old_trees:
            base = Path(str(old_dir)[: -len(old_pkg_path_str)])
            move_tree(repo, old_dir, base / pkg_path, report)
        prune_empty_dirs(repo)

        # -- 3. Rename files carrying the template token.
        for f in list(iter_files(repo)):
            if OLD_PASCAL in f.name:
                new = f.with_name(f.name.replace(OLD_PASCAL, pascal))
                if not (in_git_repo(repo) and run_git(
                        repo, "mv", str(f.relative_to(repo)), str(new.relative_to(repo)))):
                    f.rename(new)
                report["renamed"].append(f"{f.name} -> {new.name}")

        # -- 1. Global content replaces (order matters: longest first).
        last_segment = package.split(".")[-1]
        replacements = [
            (OLD_PACKAGE, package),
            (OLD_PASCAL, pascal),
            (OLD_DISPLAY, args.display_name),
            (OLD_TOKEN_LOOSE, last_segment),  # fallback for stragglers
        ]
        for f in iter_files(repo):
            if is_text_file(f):
                replace_in_file(f, replacements, report)

        # Escape apostrophes the display name may have introduced into
        # Android string resources.
        if "'" in args.display_name:
            for f in iter_files(repo):
                if f.suffix == ".xml" and "res" in f.parts and "values" in str(f):
                    text = f.read_text(encoding="utf-8")
                    fixed = text.replace(args.display_name,
                                         xml_escape_apostrophes(args.display_name))
                    if fixed != text:
                        f.write_text(fixed, encoding="utf-8")

        # -- 6. API environments (before URL swaps so the flags land). The
        # template ships all three; strip the deselected ones, keep the
        # legacy add path for older-era clones missing apiDev, then fix any
        # trigger left pointing at a removed workflow.
        for env in sorted(set(ENV_FLAVORS) - set(api_envs)):
            remove_api_env(repo, env, report)
        if "dev" in api_envs:
            add_dev_api_env(repo, report)
        if set(api_envs) != set(ENV_FLAVORS):
            report["warnings"].append(
                "api environments were removed — review docs/ for references "
                "to the removed flavors/workflows")

        # -- 6b. Firebase environments (template ships staging + live).
        if "dev" in firebase_envs:
            add_firebase_dev(repo, report)
        for env in sorted(set(FIREBASE_FLAVORS) - set(firebase_envs)):
            remove_firebase_env(repo, env, report)
        if set(firebase_envs) != {"live", "staging"}:
            report["warnings"].append(
                "firebase environments changed from the template default — "
                "review docs/ and CLAUDE.md variant/task-name references")
        remap_firebase_in_bitrise(repo, set(firebase_envs), report)
        retarget_triggers(repo, report)

        # -- 7. API URL placeholders (optional).
        gradle = repo / "app" / "build.gradle.kts"
        if args.staging_url:
            swap_api_url(gradle, "staging", args.staging_url, report)
        if args.live_url:
            swap_api_url(gradle, "live", args.live_url, report)
        if args.dev_url:
            swap_api_url(gradle, "dev", args.dev_url, report)

        # -- 9. Delete the old ruby bootstrapper.
        rb = repo / "scripts" / "build-new-project.rb"
        if rb.exists():
            if not (in_git_repo(repo) and run_git(repo, "rm", "-q", str(rb.relative_to(repo)))):
                rb.unlink()
            report["deleted"].append(str(rb.relative_to(repo)))

        # -- 10. Delete the bootstrap skill — clients must not inherit the
        # skill that creates client repos.
        skill_dir = repo / ".claude" / "skills" / "bootstrap-android"
        if skill_dir.exists():
            if not (in_git_repo(repo) and run_git(repo, "rm", "-r", "-q", str(skill_dir.relative_to(repo)))):
                shutil.rmtree(skill_dir)
            report["deleted"].append(str(skill_dir.relative_to(repo)))
            # git rm leaves the empty dirs on disk, and prune_empty_dirs
            # deliberately skips .claude (SKIP_DIRS) — tidy them here.
            for parent in (skill_dir.parent, skill_dir.parent.parent):
                if parent.is_dir() and not any(parent.iterdir()):
                    parent.rmdir()

    ok = verify(repo, report)

    report["files_edited"] = sorted(report["files_edited"])
    print(json.dumps(report, indent=2))
    if not ok:
        print("\nFAIL: template tokens or parse errors survive — see report.",
              file=sys.stderr)
        return 1
    print("\nOK: zero surviving template tokens; structured files parse.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
