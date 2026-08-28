# CI / CD

## Pull request checks (GitHub Actions)

[`pr-checks.yml`](../.github/workflows/pr-checks.yml) runs on every PR and on pushes to `master`/`develop`, and executes **ktlint only** — a deliberate team decision. Android Lint, the debug build and the test suites are local verification (see [getting-started.md](getting-started.md)); the practical consequence is that CI does not compile the app, so the first build signal after a merge comes from Bitrise. The workflow writes a minimal `gradle.properties` before running because AGP will not configure without `android.useAndroidX`.

## Signed builds and distribution (Bitrise)

[`bitrise.yml`](../bitrise.yml) defines a shared `assemble` workflow (clone, cache, Java setup, Gradle build, APK signing, deploy to Bitrise) that per-environment workflows parameterise with a `GRADLE_TASK`:

| Workflow | Variant built | Trigger |
|---|---|---|
| `assembleDevAPKS` | `firebaseStagingApiDev` release + bundle | push to `develop` |
| `assembleStagingAPKS` | `firebaseStagingApiStaging` release + bundle | push to `release/*` |
| `assembleProdAPKS` | `firebaseLiveApiLive` release + bundle | push to `hotfix/*` |
| `assembleAllAPKS` | `assembleRelease` / `bundleRelease` (everything) | manual |

The release keystore lives in Bitrise, not the repository; the bootstrap flow generates and uploads one per client. Signing is therefore not configured for local builds.

## Dependency management

[Dependabot](../.github/dependabot.yml) raises weekly grouped update PRs for Gradle dependencies (androidx, firebase and kotlin-toolchain groups) and for GitHub Actions. All versions live in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). After any dependency change, regenerate the lockfiles with `./gradlew :app:dependencies --write-locks` — builds fail with a lock-state error if this is forgotten.

## Pre-commit hook

`./gradlew installGitHook` installs a hook ([`scripts/pre-commit`](../scripts/pre-commit)) that runs ktlint before every commit, mirroring the CI check.

## Creating a client repository

New client repos are generated from this template by the `bootstrap-android` skill (interview → rename transform → verify → push), which also handles the Bitrise app, keystore and branch protection. The skill is version-controlled in this repository at [`.claude/skills/bootstrap-android`](../.claude/skills/bootstrap-android), alongside the rename transform script it drives.