# Sprint Zero Template — Android client

<!-- bootstrap-android: replace this intro with a client project header
     (client name, Jira board link, one-line app description). Everything
     below the Toolchain heading applies to generated clients as-is. -->
The 3 Sided Cube **template** for new native Android client apps. It is never
shipped itself — the `bootstrap-android` skill generates new client repos from
it (interview → rename transform → verify → push). Keep it green and current;
every future client inherits what's here.

## Toolchain

| Thing | Version | Where |
| --- | --- | --- |
| AGP | 9.3.x (built-in Kotlin) | `gradle/libs.versions.toml` |
| Gradle | 9.7.x | `gradle/wrapper/gradle-wrapper.properties` |
| JDK | 17 | `compileOptions` / CI `setup-java` |
| compileSdk / targetSdk | 37 (Android 17) | `app/build.gradle.kts` |
| minSdk | 30 | `app/build.gradle.kts` |

Versions live **only** in `gradle/libs.versions.toml` (Dependabot keeps them
fresh) — including ktlint-cli, which `ktlint.gradle` reads from the catalog.

## Build

Create a local, gitignored `gradle.properties` first (see README), then:

```
./gradlew ktlint testFirebaseStagingApiStagingDebugUnitTest \
  lintFirebaseStagingApiStagingDebug assembleFirebaseStagingApiStagingDebug
```

Those are exactly the CI tasks (`.github/workflows/pr-checks.yml`). Bitrise
(`bitrise.yml`) handles signed release builds — signing is not configured
locally.

## Non-obvious decisions (do not "clean up")

- **No `org.jetbrains.kotlin.android` plugin** — AGP 9 built-in Kotlin is used
  deliberately; no kapt/parcelize anywhere, so no opt-out is needed.
- **Hilt 2.60.1+** is the AGP 9 team baseline.
- **Selective dependency locking** (`app/build.gradle.kts`) locks only shipping
  Runtime/Compile classpaths so `app/gradle.lockfile` stays within osv-scanner
  limits. Regenerate: `./gradlew :app:dependencies --write-locks`.
- Two flavor dimensions (`firebase` × `api`); CI builds
  `firebaseStagingApiStagingDebug`.
- The committed `google-services.json` is a placeholder — never commit a real
  one.

## Known dev-machine gotcha

If `hiltJavaCompile*` fails with missing `dagger.hilt.internal.*` classes while
CI is green, a user-home `~/.gradle/gradle.properties` is leaking
`android.enableJetifier=true` (user-home outranks project properties). Build
with `-Pandroid.enableJetifier=false -Pandroid.nonTransitiveRClass=true`.
