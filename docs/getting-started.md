# Getting Started

## Prerequisites

- Android Studio (with its bundled JDK) or a standalone JDK for command-line builds.
- An Android emulator or device for running the app and the instrumented tests.

Toolchain versions are pinned in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties); do not copy them into docs or scripts.

## Setup

1. Clone the repository.
2. Create a local, gitignored `gradle.properties` at the repo root:

   ```
   kotlin.code.style=official
   org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
   android.useAndroidX=true
   android.nonTransitiveRClass=true
   ```

3. Install the ktlint pre-commit hook: `./gradlew installGitHook`. The hook runs `ktlint` before each commit; use `./gradlew ktlintFormat` to auto-fix most issues, and `removeGitHook` / `installGitHook` around merge commits if the hook gets in the way.

## Environment & secrets

- **Secrets** — copy [`secret-examples.properties`](../secret-examples.properties) to a gitignored `secret.properties` and fill in real values. The build falls back to the committed placeholders when the file is absent, so a fresh clone builds with no setup. Secrets surface in code as `BuildConfig` fields; to add one, add the row to both files and wire it in [`app/build.gradle.kts`](../app/build.gradle.kts) alongside `EXAMPLE_API_KEY`.
- **Firebase** — the committed [`google-services.json`](../app/src/firebaseStaging/google-services.json) files are placeholders. Generated clients replace them per flavor with real configs from the project's Firebase console; real configs must never be committed to a public repository.

## Build & run

Two flavor dimensions produce the variant matrix: `firebase` (`firebaseDev` / `firebaseStaging` / `firebaseLive`) × `api` (`apiDev` / `apiStaging` / `apiLive`), each with `debug` and `release` build types. Release builds are minified with R8. The everyday local variant is `firebaseStagingApiStagingDebug`:

```
./gradlew assembleFirebaseStagingApiStagingDebug
```

Local verification before pushing (CI only runs ktlint — see [ci-cd.md](ci-cd.md)):

```
./gradlew ktlint lintFirebaseStagingApiStagingDebug assembleFirebaseStagingApiStagingDebug
```

Release signing is not configured locally; Bitrise holds the keystore.

## Tests

- Unit tests (JVM, includes Robolectric): `./gradlew testFirebaseStagingApiStagingDebugUnitTest`
- Instrumented tests (needs a device/emulator): `./gradlew connectedFirebaseStagingApiStagingDebugAndroidTest`

Conventions and the example-test map are documented in [`CLAUDE.md`](../CLAUDE.md) under Testing.