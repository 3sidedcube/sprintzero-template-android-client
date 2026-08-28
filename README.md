# Template for Android

This repository provides a clean, well-organised template designed to speed up the process of setting up new projects at Cube. Use it as a starting point for your new Android project, and accelerate your progress by focusing on your app's unique features rather than boilerplate setup.

## What's baked in

- **Build**: AGP 9.x with built-in Kotlin, Kotlin DSL build files, and a version catalog (`gradle/libs.versions.toml`) as the single source of truth for versions.
- **DI**: Hilt (via KSP).
- **Firebase**: Analytics, Crashlytics and Messaging, BoM-managed. The committed `google-services.json` is a **placeholder** — see setup below.
- **Crash reporting & logging**: Firebase Crashlytics — messages and non-fatals recorded via `CrashlyticsLoggingHelper.kt`; collection is disabled in debug builds.
- **Lint**: ktlint enforced via a git pre-commit hook and in CI.
- **CI**: GitHub Actions runs ktlint on every PR (Android Lint, builds and tests run locally — see `CLAUDE.md`). Bitrise handles signed build distribution (develop/release/hotfix branch triggers).
- **Security**: dependency review on every PR; Dependabot keeps the toolchain and dependencies fresh weekly.
- **Governance**: team PR template and CODEOWNERS (`@3sidedcube/android`) included.

## New project setup

New repos are created from this template by the `bootstrap-android` Claude skill (interview → rename transform → verify → push). After bootstrap:

1. Replace `app/src/firebaseStaging/google-services.json` with the real staging config from the project's Firebase console, and add `app/src/firebaseLive/google-services.json` for the live flavor. **Real Firebase configs must never be committed to a public repo.**
2. Add a local `gradle.properties` (gitignored):

```
kotlin.code.style=official
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
```

3. Install the git hook (see below).
4. Upload the signing keystore to the Bitrise app (the bootstrap flow generates and uploads one automatically).

## Code formatting

This repo is set up with automatic git commit hooks to ensure well formatted code — credit to Ali Rezaee for the set-up. After cloning the repository, set up the hook with:

```
./gradlew installGitHook
```

The hook runs the `ktlint` task before each commit. Use `ktlintFormat` to auto-fix most issues. If the hook gets in the way of a merge commit, run `removeGitHook`, merge, then `installGitHook` again.

## Repo structure

This repo uses a single-module structure, with packages organised like a two-module split to keep interests clean:

- `app` — the main application plus a package per distinct element of the app's UI flow (activities, fragments, adapters used exclusively in that part of the app).
- `lib` — non-UI logic (config, API logic) and generic UI logic reused across the app (views, viewholders).

## Flavors

Two flavor dimensions: `firebase` (`firebaseStaging` / `firebaseLive`) and `api` (`apiDev` / `apiStaging` / `apiLive`). CI builds `firebaseStagingApiStagingDebug`; Bitrise workflows map branches to variants (see `bitrise.yml`).

## Secrets

API keys and other secrets live in a gitignored `secret.properties` at the repo root, exposed to code as `BuildConfig` fields. Set it up with:

```
cp secret-examples.properties secret.properties
```

then fill in the real values. When `secret.properties` is absent the build falls back to the committed `secret-examples.properties` placeholders, so fresh clones and CI build without setup. To add a secret: add the row to **both** files (placeholder in the example, real value locally) and wire it in `app/build.gradle.kts` as a `buildConfigField` alongside `EXAMPLE_API_KEY`. Never commit `secret.properties` or put real values in the example file.

## Firebase integration

The template uses Firebase for push notifications, analytics and crash reporting. Create the project under the 3SidedCube account and drop the per-flavor `google-services.json` files in as described above.

## Logging

Logging goes straight to Firebase Crashlytics — see `CrashlyticsLoggingHelper.kt`. `logError` records a non-fatal exception (with an optional context message); `logInfo` writes a breadcrumb that is attached to the next crash or non-fatal report. Crashlytics collection is disabled in debug builds (`SprintZeroTemplateApp`), so nothing is reported from local dev sessions.

## CI reference

| Concern | Where |
| --- | --- |
| PR checks (ktlint) | `.github/workflows/pr-checks.yml` |
| Dependency review | `.github/workflows/security.yml` |
| Dependency updates | `.github/dependabot.yml` |
| Signed builds, distribution | `bitrise.yml` |
