# Template for Android

This repository provides a clean, well-organised template designed to speed up the process of setting up new projects at Cube. Use it as a starting point for your new Android project, and accelerate your progress by focusing on your app's unique features rather than boilerplate setup.

## What's baked in

- **Build**: AGP 9.x with built-in Kotlin, Kotlin DSL build files, and a version catalog (`gradle/libs.versions.toml`) as the single source of truth for versions.
- **DI**: Hilt (via KSP).
- **Firebase**: Analytics, Crashlytics and Messaging, BoM-managed. The committed `google-services.json` is a **placeholder** — see setup below.
- **Crash reporting**: Sentry, fully integrated with Timber. Production exceptions flow to Sentry with no extra wiring.
- **Logging**: Timber, preconfigured (`TimberLoggingHelper.kt`).
- **Lint**: ktlint enforced via a git pre-commit hook and in CI.
- **CI**: GitHub Actions runs ktlint, unit tests, Android Lint and a debug build on every PR. Bitrise handles signed build distribution (develop/release/hotfix branch triggers, Jira build comments, Slack notifications).
- **Security**: gitleaks secret scanning and dependency review on every PR; Dependabot keeps the toolchain and dependencies fresh weekly.
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
systemProp.SENTRY_DSN={{Your Sentry URL}}
```

3. Install the git hook (see below).
4. Configure Bitrise secrets: `JIRA_API_TOKEN`, `JIRA_BASE_URL`, `JIRA_EMAIL_USER`, `SLACK_WEBHOOK`, signing keystore.

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

Two flavor dimensions: `firebase` (`firebaseStaging` / `firebaseLive`) and `api` (`apiStaging` / `apiLive`). CI builds `firebaseStagingApiStagingDebug`; Bitrise workflows map branches to variants (see `bitrise.yml`).

## Firebase integration

The template uses Firebase for push notifications, analytics and crash reporting. Create the project under the 3SidedCube account and drop the per-flavor `google-services.json` files in as described above.

## Sentry & Timber

Timber is installed and configured — see `TimberLoggingHelper.kt`. In production, exceptions and crashes are sent to Sentry via the Timber integration; no additional calls required.

## CI reference

| Concern | Where |
| --- | --- |
| PR checks (ktlint, unit tests, lint, debug build) | `.github/workflows/pr-checks.yml` |
| Secret scanning & dependency review | `.github/workflows/security.yml` |
| Dependency updates | `.github/dependabot.yml` |
| Signed builds, distribution, Jira/Slack | `bitrise.yml` |
