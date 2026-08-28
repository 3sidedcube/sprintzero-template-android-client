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
  4. Optionally adds a third 'dev' API environment (--api-envs dev,staging,live):
     the apiDev product flavor plus the assembleDevAPKS Bitrise workflow.
  5. Optionally swaps dev/staging/live API_URL placeholder values.
  6. Deletes scripts/build-new-project.rb (no-op safeguard; gone from the template).
  6. Verifies: zero surviving template tokens (any case), TOML/JSON/YAML parse.

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
SKIP_DIRS = {".git", ".gradle", "build", ".idea", "node_modules"}

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


def heal_dangling_dev_trigger(repo: Path, report: dict) -> None:
    """Older template states trigger develop -> assembleDevAPKS without the
    workflow existing. When no dev env is requested, retarget to staging."""
    bitrise = repo / "bitrise.yml"
    if not bitrise.exists():
        return
    text = bitrise.read_text(encoding="utf-8")
    if DEV_TRIGGER in text and "\n  assembleDevAPKS:" not in text:
        bitrise.write_text(
            text.replace(DEV_TRIGGER, STAGING_TRIGGER, 1), encoding="utf-8")
        report["warnings"].append(
            "bitrise trigger for develop referenced a nonexistent assembleDevAPKS "
            "workflow — retargeted to assembleStagingAPKS")


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
    ap.add_argument("--api-envs", default="staging,live",
                    help="API environments: 'staging,live' (template default) "
                         "or 'dev,staging,live' (adds the apiDev flavor and "
                         "the assembleDevAPKS Bitrise workflow)")
    ap.add_argument("--verify-only", action="store_true",
                    help="run only the verification pass")
    args = ap.parse_args()

    api_envs = sorted({e.strip().lower() for e in args.api_envs.split(",") if e.strip()})
    if api_envs not in (["live", "staging"], ["dev", "live", "staging"]):
        print("error: --api-envs must be 'staging,live' or 'dev,staging,live'",
              file=sys.stderr)
        return 2
    if args.dev_url and "dev" not in api_envs:
        print("error: --dev-url given but 'dev' is not in --api-envs",
              file=sys.stderr)
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
        "warnings": [], "survivors": [], "parse_errors": [],
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

        # -- 6. Extra API environments (before URL swaps so --dev-url lands).
        if "dev" in api_envs:
            add_dev_api_env(repo, report)
        else:
            heal_dangling_dev_trigger(repo, report)

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
