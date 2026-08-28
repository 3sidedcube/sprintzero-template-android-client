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
fresh) — including ktlint-cli, which the ktlint tasks in `app/build.gradle.kts`
read from the catalog.

## Build

Create a local, gitignored `gradle.properties` first (see README), then:

```
./gradlew ktlint testFirebaseStagingApiStagingDebugUnitTest \
  lintFirebaseStagingApiStagingDebug assembleFirebaseStagingApiStagingDebug
```

Those are exactly the CI tasks (`.github/workflows/pr-checks.yml`). Bitrise
(`bitrise.yml`) handles signed release builds — signing is not configured
locally.

Secrets follow the `secret.properties` (gitignored) / `secret-examples.properties`
(committed placeholders, automatic fallback) pattern — see README. Real values
never go in the example file.

## Code standards

- **Kotlin only** for new code. Defaults: ViewBinding + ViewModel, Hilt for DI.
  **No event bus** — prefer Flow/LiveData/direct callbacks.
- **Edge-to-edge everywhere** (enforced from targetSdk 35): `ViewBindingActivity`
  calls `enableEdgeToEdge()`; keep content and touch targets clear of system
  bars with the `applySystemBarInsetsAsPadding`/`AsMargin` extensions in
  `lib/extensions/EdgeToEdgeExtensions.kt`. Remember the horizontal insets — in
  landscape the nav bar sits on a side edge. At most one inset call per view
  (combine edge flags; a second call replaces the listener); scrolling views
  pair bottom padding with `clipToPadding="false"`; opt into `includeIme` on
  screens with text input. No legacy `windowTranslucentNavigation`/
  `statusBarColor` theme flags.
- **Package split:** `app` = UI flow (activities/fragments/adapters), `lib` =
  non-UI logic and reusable UI (see README "Repo structure").
- **`.editorconfig` is the style source of truth** (tabs, max line 200,
  `ktlint_code_style = android_studio`); ktlint enforces it in CI and the
  pre-commit hook. Avoid local (inline) functions that capture enclosing
  state — prefer a private function on the class (team review convention).
- **No hardcoded user-visible strings** in layouts or Kotlin — including
  `contentDescription`, hints and dialog text. Everything goes through
  `res/values/strings.xml`. (CMS-driven clients swap the store, not the rule.)
- **No hardcoded colours or dimensions** in layouts or Kotlin — every colour
  is a `@color/` token, every dimension a `@dimen/` token (starter scale in
  `app/src/main/res/values/dimens.xml`). Naming: `spacing_<N>dp` for the
  generic scale, `<feature>_<purpose>` / `<component>_<purpose>` for specific
  tokens. Text sizes use `sp`, never `dp`. Pre-PR check:
  `grep -rE '#[0-9A-Fa-f]{6,8}|"[0-9]+dp"' app/src/main/res/layout/` should
  turn up nothing new.

## Accessibility requirements (WCAG 2.x AA)

Not optional — factor the time into estimates. For every new screen,
component and modal:

- **TalkBack:** meaningful `contentDescription` on every interactive element;
  decorative images get `android:importantForAccessibility="no"`; group
  related views so a row reads as one announcement; modals/bottom sheets
  announce themselves and offer a focusable close button (swipe-to-dismiss
  alone is not accessible); focus order follows visual reading order; custom
  views implement an accessibility delegate. Verify on a real device with
  TalkBack on.
- **Font scaling:** text sizes in `sp` only; every screen scrolls; no fixed
  heights on text-bearing views; avoid `maxLines` + ellipsize on critical
  content; must stay usable at 200% font scale (WCAG 1.4.4).
- **Touch targets:** ≥ 48×48dp for anything tappable (WCAG 2.5.5).
- **Contrast:** ≥ 4.5:1 for normal text, ≥ 3:1 for large text and UI
  components (WCAG 1.4.3); pull colours from `colors.xml` tokens.
- **Both orientations:** every screen works in portrait and landscape —
  never lock orientation.

Pre-PR self-check: TalkBack walk of the flow → max font size (Display AND
Accessibility settings) → rotate to landscape → tap targets ≥ 48dp → the
ticket's WCAG criteria.

## Testing

- **Unit tests** (`app/src/test`, package-mirrored): plain JUnit + MockK for pure
  logic and Firebase-touching code; Robolectric (`@RunWith(AndroidJUnit4::class)`)
  when a real Context/View/resources are needed. Robolectric simulates API 36 and
  runs against `HiltTestApplication` (`app/src/test/resources/robolectric.properties`)
  so the real Application (Hilt + Firebase init) never executes on the JVM.
- **Instrumented tests** (`app/src/androidTest`): Espresso, launched through
  `HiltTestRunner`; `@HiltAndroidTest` + `HiltAndroidRule` (order 0) +
  `ActivityScenarioRule` (order 1).
- **Never touch real Firebase in tests** — mock the `Firebase.x` accessor with
  `mockkStatic` (see `CrashlyticsLoggingHelperTest`) or stub the helper object
  with `mockkObject` (see `BootActivityTest`).
- Naming: `method_scenario_expectedOutcome`, one behaviour per test. Every
  production class has an example test to crib from.
- Run: `./gradlew testFirebaseStagingApiStagingDebugUnitTest` (the CI task) and
  `./gradlew connectedFirebaseStagingApiStagingDebugAndroidTest` (needs a device).

## When making changes

- **New dependency** → version in `gradle/libs.versions.toml`, reference via
  `libs.xxx`, regenerate the lockfile (`./gradlew :app:dependencies --write-locks`).
- **New permission / activity / deep link** → `app/src/main/AndroidManifest.xml`;
  mind `android:exported`, `parentActivityName` and theme.
- **Flavor-specific resource** → `app/src/<flavor>/…` (e.g. the per-flavor
  `google-services.json`).

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
