# Dependencies

Major third-party libraries and what each is for. Transitive and trivial dependencies are omitted. Versions are intentionally not listed — [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) is the single source of truth for those (Dependabot keeps them fresh).

| Library | Use-case |
|---|---|
| AndroidX core / appcompat / activity / fragment | Platform compatibility layer and Kotlin extensions used throughout |
| AndroidX Lifecycle (runtime, viewmodel) | Lifecycle-aware components; ViewModel is the team default for screen state |
| Jetpack Navigation (fragment, ui) | Tab and screen navigation via nested navigation graphs |
| ConstraintLayout / RecyclerView | Standard layout and list building blocks |
| Material Components | Material 3 theme and widgets, including the bottom navigation bar |
| AndroidX Core SplashScreen | The Android 12+ splash screen API used by the boot flow |
| Hilt (with KSP compilers) | Dependency injection |
| Lottie | Vector animation playback for client apps that need it |
| Firebase BoM: Analytics, Crashlytics, Messaging | Usage analytics, crash reporting and push messaging |
| JUnit 4 + AndroidX Test | Test framework for the unit and instrumented suites |
| MockK | Mocking in unit tests, including static mocking of the Firebase accessors |
| Robolectric | JVM-side Android runtime so Context/View-dependent code is unit-testable |
| Espresso | Instrumented UI tests |
| Hilt testing | `HiltTestApplication` / rules for testing `@AndroidEntryPoint` classes |
| ktlint CLI | Code style enforcement, run via the `ktlint`/`ktlintFormat` Gradle tasks |