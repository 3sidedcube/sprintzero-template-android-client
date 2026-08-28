---
name: bootstrap-android
description: >-
  Bootstrap a new native Android project for 3 Sided Cube from the
  sprintzero-template-android-client template: interview, create the private
  repo in the 3sidedcube org, rename/parameterise, verify, push, protect the
  branches and open the setup checklist. Use whenever someone asks to create,
  spin up, kick off, start, or bootstrap a new Android app, project, repo, or
  client build — even if they only say "new Android app for so-and-so". Not
  for iOS or React Native (separate skills), and not for adding features to
  an existing repo.
---

# bootstrap-android

One conversation in, one repo out. The team member answers a short interview
and gets back a private repo in `3sidedcube`, created from
`3sidedcube/sprintzero-template-android-client`, fully renamed, verified,
protected, with a setup-checklist issue for the human-only tasks.

Standards live in the template, not here: never regenerate CI or build config
from memory. This skill's job is the interview, the mechanical transform
(`scripts/transform.py`), the GitHub plumbing, and the handover.

## Surface detection

Decide up front which surface you are on and say so in the final report:

- **Claude Code / a machine with an Android SDK**: full verification — run the
  real Gradle build before pushing (Step 4, level "compiled").
- **claude.ai / no Android SDK**: static verification only (level "static");
  compile verification is delegated to the first CI run. State this plainly.

## Step 0 — Interview & validation

Ask for everything in **one batch**, not a drip-feed. Validate hard before
touching GitHub — a bad package name poisons every file.

| Parameter | Placeholder | Validation / default |
| --- | --- | --- |
| App display name | `<Display Name>` | Free text. Derive PascalCase (`<DisplayName>`) for class/theme names; confirm the derivation with the user if ambiguous. |
| Package / applicationId | `com.cube.<appname>` | Lowercase dot-separated segments, each `[a-z][a-z0-9]*`. Default prefix `com.cube.`. |
| Repo name | `<app-name>-android-client` | Kebab-case. Must not already exist in `3sidedcube`. |
| Client name | `<Client>` | Used in the README header, repo topic, and kickoff issue. |
| Jira project key | `<KEY>` | Optional. Never invent one. Enables epic creation and branch/PR conventions from day one. When given, record the board link in the client CLAUDE.md header (Step 3) so agents can find it. |
| Figma project | `<figma.com link>` | Optional. Never invent one. When given, record the design-file link in the client CLAUDE.md header (Step 3) so agents (and the Figma MCP tooling) can find the designs. |
| API environments | `dev + staging + live` (all on) | Multi-select with **all three toggled on** — the user switches off the ones they don't want (any non-empty subset is valid). The template ships all three; the transform removes each deselected environment's flavor and Bitrise workflow and retargets branch triggers to the nearest surviving workflow (`--api-envs <kept,envs>`). Firebase is **always** exactly staging + live regardless (2 Firebase projects by policy — do not ask). |
| API base URLs (per env) | `https://api.staging...` | Optional; template placeholders remain if unknown. One per chosen API environment (`--staging-url` / `--live-url` / `--dev-url`). |
| Firebase projects | `create now` / `manual` | Two per app — `<appname>-staging` + `<appname>-live` — always. If firebase-tools is authenticated (`firebase login:list`), offer to create both under the 3SC org now (Step 7); otherwise record the intended ids for the checklist. |
| Bitrise app | `create now` / `manual` | If the Bitrise PAT is available (macOS keychain, `security find-generic-password -s bitrise-pat -w`), offer to register the app, wire the webhook and generate/upload the signing keystore now (Step 8); otherwise the checklist keeps the manual Bitrise items. |
| GA account id | `56643500` | Default: **3 Sided Cube's own GA account `56643500`** (name verified in the GA console, 2026-08-25) — analytics start under 3SC and get transferred to the client's account later when required (GA4 property move). Override only if the client should own analytics from day one — then discover their id from an existing project's `analyticsDetails` (see Step 7); never guess a client id. |
| minSdk override | `30` | Default: template value (30). |

`scripts/transform.py` enforces the package and PascalCase rules and will
exit 2 on bad input — but validate in conversation first so the user isn't
told after the repo exists.

## Step 1 — Preflight

Stop at the first failure; report, don't improvise.

1. GitHub auth works (on a Cube VPS, follow the `gh-protocol` skill first).
2. The template's default branch contains `gradle/libs.versions.toml`. If
   absent: **stop** — the AGP-9 refresh PR has not been merged and
   bootstrapping from the old template era is not allowed.
3. Target repo name is free in `3sidedcube`.

## Step 2 — Create the repo from the template

```
gh repo create 3sidedcube/<repo-name> \
  --template 3sidedcube/sprintzero-template-android-client --private
```

(or the API equivalent: `POST /repos/3sidedcube/sprintzero-template-android-client/generate`).
Private by default, always in the org, never personal. Record the template's
current default-branch commit SHA now — it goes in the bootstrap commit
message.

Template generation is **asynchronous** — an immediate clone can come back
empty (and leave a stray local `main` branch). Poll until the default branch
exists before cloning or checking out:

```
until git ls-remote --heads origin master | grep -q master; do sleep 3; done
```

Expect Dependabot to open a batch of dependency-bump PRs within minutes of
creation (the template's weekly schedule fires immediately on new repos).
This is normal — leave them for the team to triage and mention them in the
handover so nobody mistakes them for a problem.

## Step 3 — Clone and transform

Clone the new repo, then run the deterministic transform — do **not** perform
the renames as freehand edits:

```
python3 scripts/transform.py --repo <clone> \
  --display-name "<Display Name>" --package <package> \
  [--api-envs dev,staging,live] \
  [--staging-url <url>] [--live-url <url>] [--dev-url <url>]
```

`--api-envs` defaults to `dev,staging,live` — the template's flavors, so the
default is a no-op. Pass the kept subset from the interview: deselected
environments have their `api<Env>` flavor and `assemble<Env>APKS` Bitrise
workflow removed, and any branch trigger left pointing at a removed workflow
is retargeted to the nearest surviving one (staging, then live, then dev).
All idempotent on re-runs.

Run it with **Python 3.11+** when available — the TOML parse check needs
`tomllib`. On macOS the system `python3` is often 3.9; prefer a Homebrew
interpreter (e.g. `/opt/homebrew/bin/python3.13`). On older Pythons the
script skips the TOML check with a warning instead of failing — if you see
that warning, rerun `--verify-only` with a newer interpreter before calling
verification complete.

It performs the full rename inventory: global token replaces
(`com.cube.sprintzerotemplate` → package, `SprintZeroTemplate` → PascalName,
`Sprint Zero Template` → display name), moves the three source trees via
`git mv`, renames `SprintZeroTemplateApp.kt`, swaps API_URL placeholders,
deletes `scripts/build-new-project.rb` if present (already gone from the
template since 2026-08-25 — the delete is a no-op safeguard), then verifies
zero surviving template tokens and that TOML/JSON/YAML still parse. Non-zero
exit = stop and fix; the JSON report says exactly what survived. Read its
`warnings` — an API_URL it could not locate must be set by hand.

Three edits the script deliberately leaves to you:

- **README**: the template README is a short description plus a Documentation
  table linking into `docs/` — replace the description with a client project
  header (client name and Jira board link) and keep the table. Refresh
  `docs/product-overview.md`'s template-speak with the client context while
  you're there; the other docs describe the code and transform cleanly.
- **CLAUDE.md**: the transform renames its heading, but the intro paragraph
  still describes the template — replace it with a client project header per
  the `<!-- bootstrap-android: ... -->` marker comment in the file (client
  name, one-line app description, and — when the interview provided them —
  the Jira board link and the Figma design-file link, so agents working in
  the client repo can find both). Keep the Toolchain section and everything
  below as-is.
- **minSdk**: only if the user overrode it.

## Step 4 — Verify

- **Claude Code**: first create the local `gradle.properties` the README
  prescribes (it is gitignored):

  ```
  kotlin.code.style=official
  org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
  android.useAndroidX=true
  android.nonTransitiveRClass=true
  ```

  then build with explicit CLI overrides — a user-home
  `~/.gradle/gradle.properties` outranks project properties, and stale global
  flags (jetifier especially) break AGP 9 builds that are green in CI:

  ```
  ./gradlew ktlint testFirebaseStagingApiStagingDebugUnitTest \
    assembleFirebaseStagingApiStagingDebug \
    -Pandroid.enableJetifier=false -Pandroid.nonTransitiveRClass=true
  ```

  Known failure signature: `package dagger.hilt.internal.componenttreedeps
  does not exist` (or other missing `dagger.hilt.internal.*` symbols) from a
  `hiltJavaCompile*` task means `android.enableJetifier=true` leaked in from
  the user's global gradle.properties — the CLI overrides above fix it
  without touching their file; suggest they remove the stale flags but never
  edit `~/.gradle/gradle.properties` yourself. Fix forward until green.
- **claude.ai**: rerun `transform.py --verify-only` (zero-token + parse
  checks) and state that compile verification is delegated to CI.

## Step 5 — Push

Conventional commit(s) on `master` (the template's default branch — keep it),
e.g.:

```
chore: bootstrap <Display Name> from sprintzero template

Template: 3sidedcube/sprintzero-template-android-client @ <sha>
```

Recording the template SHA makes future template upgrades diffable. Never
force-push, never rewrite history.

## Step 6 — Branches & repo settings

1. **Branch model**: `master` stays the default/main branch (releases land
   there). Create a `develop` branch from `master` and push it — `develop`
   is the working branch: features branch off it and PRs merge into it
   (Bitrise maps develop/release/hotfix triggers). Do **not** rename the
   default branch to `develop`.
2. Branch protection on **both** `master` and `develop`: require the
   `PR Checks` workflow job — the status-check context is the job display
   name (`ktlint`; CI deliberately runs nothing else) — plus one review.
   No force pushes, no deletions.
3. Repo topics: `android` + a client tag.
4. Confirm the "template repository" flag is **off** on the new repo.

## Step 7 — Firebase projects (optional automation)

Runs only if the user opted in at the interview **and** `firebase login:list`
shows an authenticated account (CLI: `npm i -g firebase-tools`). If either is
false, skip — the checklist keeps the manual Firebase items. Firebase
failures never block the bootstrap: report what failed and fall back to the
manual checklist items.

1. Create both projects (no `-o` flag — 3SC does not parent projects under a
   GCP organization node; decided 2026-08-25, matching the existing estate):

   ```
   firebase projects:create <appname>-staging -n "<Display Name> Staging"
   firebase projects:create <appname>-live    -n "<Display Name> Live"
   ```

   GCP caps project **display names at 30 characters** — if
   `<Display Name> Staging` exceeds that, abbreviate sensibly (e.g.
   "KB Test3 Staging") and record the abbreviation in the report. The
   project *id* is what matters; the display name is cosmetic.

   If the CLI unexpectedly prompts for a parent resource, choose "No
   organization" and note it in the report. Project ids are **globally
   unique**: on a collision, ask the user for an alternative — never invent
   one silently. Creation counts against the authenticated user's project
   quota, so never create throwaway projects.

2. Register the Android app in each (both flavors share one applicationId):

   ```
   firebase apps:create android "<Display Name>" -a <package> --project <appname>-staging
   firebase apps:create android "<Display Name>" -a <package> --project <appname>-live
   ```

3. **Link Google Analytics** — CLI-created projects have GA unlinked (the
   console create-flow normally does this). Default account: **3SC's own
   `56643500`** per the team policy (start under 3SC, transfer the GA4
   property to the client's account when required). Client-owned accounts
   exist too (ARC → 31948082, Glastonbury → 312389681, Omaze → 32530319) —
   if the interview said the client owns analytics from day one, discover
   their id from an existing project of theirs
   (`GET https://firebase.googleapis.com/v1beta1/projects/<existing-id>/analyticsDetails`
   → `analyticsProperty.analyticsAccountId`) and **never guess**. The
   firebase CLI has no command for linking; call the Management API with
   the CLI's stored session:

   ```
   node -e "const auth=require('firebase-tools/lib/auth');(async()=>{const a=auth.getGlobalDefaultAccount();const t=await auth.getAccessToken(a.tokens.refresh_token,[]);for(const p of ['<appname>-staging','<appname>-live']){const r=await fetch('https://firebase.googleapis.com/v1beta1/projects/'+p+':addGoogleAnalytics',{method:'POST',headers:{Authorization:'Bearer '+t.access_token,'Content-Type':'application/json'},body:JSON.stringify({analyticsAccountId:'<gaAccountId>'})});const j=await r.json();console.log(p,j.error?j.error.status:'linked');}})()"
   ```

   (Leans on firebase-tools internals — `lib/auth` — verified on 15.28.1;
   run from a directory where `firebase-tools` resolves, or use the global
   install path.) **PERMISSION_DENIED means the runner lacks the Editor
   role on that GA account** — a Google Analytics grant, separate from
   Firebase/GCP project rights, typically held by the client or whoever set
   up their analytics. Fall back to a checklist item linking to
   `https://console.firebase.google.com/project/<appname>-live/settings/integrations/analytics`
   (and the staging twin) and note who to ask for GA access.

4. **Enable the Crashlytics service** on both projects (project-level rights
   suffice — no GA-style grant needed). Same token pattern, POST to:

   ```
   https://serviceusage.googleapis.com/v1/projects/<projectId>/services/firebasecrashlytics.googleapis.com:enable
   ```

   The Crashlytics console dashboard still activates only on the first crash
   report — there is no public API for that; this just removes the
   server-side blocker.

5. Fetch the real configs straight into the flavor source sets, passing the
   app ids from step 2's output. Two traps, both hit on the first live run:
   `-o` refuses to overwrite an existing file, so the committed staging
   placeholder must be deleted first; and the live source set doesn't exist
   yet:

   ```
   rm app/src/firebaseStaging/google-services.json
   mkdir -p app/src/firebaseLive
   firebase apps:sdkconfig android <stagingAppId> --project <appname>-staging -o app/src/firebaseStaging/google-services.json
   firebase apps:sdkconfig android <liveAppId>    --project <appname>-live    -o app/src/firebaseLive/google-services.json
   ```

   Sanity-check both files (project_id and package_name match), then prove
   they parse in the build:
   `./gradlew processFirebaseStagingApiStagingDebugGoogleServices
   processFirebaseLiveApiLiveDebugGoogleServices`.

6. Commit both configs — **only after verifying the repo is private and
   org-owned** (`gh api repos/3sidedcube/<repo> --jq .private` must be
   `true`; refuse otherwise):

   ```
   build(firebase): add staging and live google-services.json
   ```

7. **Environment type cannot be automated** — it lives only in the Firebase
   console's private backend (verified empirically 2026-08-25: flipping it
   changes neither the Management API `annotations` nor the GCP labels). Add
   a checklist item telling a human to mark the live project as
   **Production**, with the direct link:
   `https://console.firebase.google.com/project/<appname>-live/settings/general`.
   Staging stays "Unspecified" — that's correct.

8. Record both project ids for the handover report and drop the manual
   Firebase items from the checklist in favour of "created: `<ids>`".

## Step 8 — Bitrise app (optional automation)

Runs only if the user opted in at the interview **and** the Bitrise PAT is
available in the macOS keychain
(`security find-generic-password -s bitrise-pat -w`; it authenticates the
shared `3SidedCube` Bitrise account). If either is false, skip; the checklist
keeps the manual Bitrise items. Bitrise failures never block the bootstrap:
report what failed and fall back to the manual checklist items. API host: `https://api.bitrise.io/v0.1`, header
`Authorization: <PAT>`. Workspace: **`c3fb7a679c51271a`** ("3 Sided Cube" —
the one with the real client apps; the other two workspaces are vestigial).

1. Register with the **hybrid connection** — both halves are load-bearing
   (established empirically 2026-08-25): `provider: github-app` gives the
   server-side service credential that repository-stored config requires
   (plain `github` cannot read the repo → the storage flip 500s), while the
   **SSH repo URL + deploy key** gives the build VM its clone (the org's
   Bitrise GitHub App installation is repo-*selected* and only an org owner
   can extend it, so its token does not cover new repos):

   ```
   ssh-keygen -t rsa -b 4096 -m PEM -N "" -f <keyfile>   # MUST be RSA in PEM — ed25519/OpenSSH format is rejected
   POST /apps/register
     {"provider":"github-app","repo_url":"git@github.com:3sidedcube/<repo>.git",
      "git_repo_slug":"<repo>","git_owner":"3sidedcube","is_public":false,
      "type":"git","organization_slug":"c3fb7a679c51271a","title":"<repo>"}
   POST /apps/<app-slug>/register-ssh-key
     {"auth_ssh_private_key":"<priv>","auth_ssh_public_key":"<pub>",
      "is_register_key_into_provider_service":false}
   gh api -X POST repos/3sidedcube/<repo>/keys -f "title=Bitrise (bootstrap)" \
     -f "key=<pub>" -F read_only=true
   POST /apps/<app-slug>/finish
     {"project_type":"android","stack_id":"osx-xcode-15.3.x",
      "mode":"manual","config":"default-android-config",
      "organization_slug":"c3fb7a679c51271a"}
   ```

   `finish` returns a `build_trigger_token` — keep it for the webhook. The
   `default-android-config` placeholder is required by the API and becomes
   irrelevant at the next step. Delete the local private key file after
   registration — Bitrise holds it; nothing else needs it.

2. Make the **repo's `bitrise.yml` the live config** (team convention: CI
   config is versioned in the repo, zero drift):

   ```
   PUT /apps/<app-slug>/bitrise.yml/config   {"location": "repository"}
   ```

   Verify the readback says `{"location":"repository"}`. Trigger resolution
   and workflows now come from the repo file on every build — config changes
   ship by committing, with no Bitrise-side pushes. Prerequisite: the repo
   file must be valid — bootstraps from template ≥ 2026-08-25 carry the
   team stack (`osx-xcode-15.3.x` / `machine_type_id: g2.mac.large` — "Xcode
   15.3 Large", the OS-update standard), the guarded `activate-ssh-key`, no
   Jira/Slack steps and no unit-test step (tests are a local/dev workflow —
   GitHub Actions CI runs ktlint only); on older-era repos fix those in the
   repo first (never via a Bitrise-side copy).

3. Webhook — tokenless-git registration reports
   `is_webhook_auto_reg_supported: false`, so wire GitHub directly:

   ```
   gh api -X POST repos/3sidedcube/<repo>/hooks -f name=web -F active=true \
     -f "events[]=push" -f "config[content_type]=json" \
     -f "config[url]=https://hooks.bitrise.io/h/github/<app-slug>/<build_trigger_token>"
   ```

4. Signing keystore — generate, upload, confirm, and put the credentials in
   the handover for immediate vaulting (team decision 2026-08-25; the
   passwords deliberately transit the report):

   ```
   keytool -genkeypair -keystore <appname>.keystore -storetype PKCS12 \
     -storepass <ONE openssl rand -hex 16 password> \
     -alias <appname> -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Duncan Cook, OU=Mobile, O=3SidedCube, L=Bournemouth, ST=Dorset, C=GB"
   POST /apps/<app-slug>/android-keystore-files
     {"upload_file_name":"<appname>.keystore","upload_file_size":<bytes>,
      "password":"<storepass>","alias":"<appname>","private_key_password":"<storepass>"}
   # PKCS12 has a SINGLE password: keytool silently ignores a separate
   # -keypass, so the key is protected by the store password — pass it as
   # both fields or apksigner fails with "final block not properly padded".
   curl -T <appname>.keystore '<upload_url from response>'   # presigned S3, 10-min validity
   POST /apps/<app-slug>/android-keystore-files/<file-slug>/uploaded
   ```

   Never commit the keystore; the checklist tells a human to vault the file
   + passwords in 1Password (Bitrise also retains the file).

5. **No secrets are required** — the Jira/Slack post-build steps were
   removed from the template's workflow (2026-08-25), and they were the only
   consumers of `JIRA_*`/`SLACK_WEBHOOK`. If a client app later needs custom
   secrets, values are copyable from donor apps via
   `GET /apps/<donor-slug>/secrets/<name>/value` (unprotected secrets only)
   and written with `PUT /apps/<app-slug>/secrets/<name>`.

6. Record the Bitrise app URL (`https://app.bitrise.io/app/<app-slug>`) for
   the report and trim the checklist accordingly.

## Step 9 — Handover

Open a `chore: project setup checklist` issue listing the human-only tasks:

- Firebase: if Step 7 ran, list the created project ids and note the real
  configs are committed; if it didn't, keep the manual items — create
  `<appname>-staging` + `<appname>-live` under the 3SidedCube account and
  add the real staging + live `google-services.json` (one per firebase
  flavor).
- Bitrise: if Step 8 ran — vault the generated keystore + passwords in
  1Password (from the handover report); no secrets are needed since the
  Jira/Slack steps were removed from the workflow. If it didn't run — full
  manual Bitrise app creation + keystore.
- Triage the Dependabot PRs opened at repo creation.
- Rotate anything sensitive that was shared during setup.

Then report back to the user: repo URL, issue URL, branch model
(`master` default / `develop` working), and the verification level achieved
(compiled vs static/CI-pending).

(Hive integration is deferred for now. When it's re-enabled, this step also
pulls a short, cited client brief from the client's Hive space into the
README and this report.)

## Failure handling

If any step fails — auth, permissions, name collision, surviving tokens,
red build — stop and report what happened and what remains. Do not work
around auth, do not retry destructively, do not leave the repo half-renamed
without saying so.

## Guardrails (mirror org rules — non-negotiable)

- Never rewrite git history (no force-push, rebase, amend, reset of pushed state).
- Any PRs this skill opens are **drafts**.
- Never commit a signing key or token. Real `google-services.json` files may
  be committed **only** to the freshly generated client repo, only by Step 7's
  automation, and only after verifying the repo is private and org-owned —
  never to the template, never to any public repo.
- Never invent Jira keys.
- Repos are private and org-owned, never personal.
