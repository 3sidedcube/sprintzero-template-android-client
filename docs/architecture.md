# Architecture

## Overview

A single-module application (`:app`) organised as a classic multi-activity app with Fragments, ViewBinding and Hilt. The team default for new screens is ViewBinding + ViewModel (MVVM); the template's placeholder screens are simple enough that no ViewModel exists yet. Packages are organised as a two-package split that behaves like a module boundary.

## Module / layer map

| Package | Responsibility |
|---|---|
| [`app`](../app/src/main/java/com/cube/sprintzerotemplate/app) | The application class plus a package per distinct element of the UI flow — activities and fragments used exclusively in that part of the app |
| [`lib`](../app/src/main/java/com/cube/sprintzerotemplate/lib) | Non-UI logic and reusable pieces: extensions, generic base classes, preferences, services and util helpers |

Code in `app` may depend on `lib`; the reverse must not happen.

## Navigation

Jetpack Navigation with one nested graph per tab, included by [`main_navigation.xml`](../app/src/main/res/navigation/main_navigation.xml). [`MainTabbedActivity`](../app/src/main/java/com/cube/sprintzerotemplate/app/activities/MainTabbedActivity.kt) hosts the `NavHostFragment` and wires the `BottomNavigationView` to the controller; menu item ids in [`bottom_nav.xml`](../app/src/main/res/menu/bottom_nav.xml) must match the included graph ids for the wiring to work. Reselecting the current tab pops that tab's stack back to its root. App entry is [`BootActivity`](../app/src/main/java/com/cube/sprintzerotemplate/app/activities/BootActivity.kt), which holds the splash screen briefly and then starts the tabbed shell.

## Networking

There is no networking layer. Each `api` flavor injects an `API_URL` placeholder as a `BuildConfig` field ([`app/build.gradle.kts`](../app/build.gradle.kts)); the client project adds its own client (the team convention is Retrofit) on top.

## Persistence

`SharedPreferences` only, wrapped by the [`GlobalAppPreferences`](../app/src/main/java/com/cube/sprintzerotemplate/lib/preferences/GlobalAppPreferences.kt) singleton, which is initialised in the application class. There is no database; clients add Room if needed.

## Dependency injection

Hilt. The application class is `@HiltAndroidApp` and activities/fragments are `@AndroidEntryPoint`; processors run through KSP (AGP's built-in Kotlin — see the non-obvious decisions in [`CLAUDE.md`](../CLAUDE.md)).

## Cross-cutting concerns

- **Crash reporting and logging** — [`CrashlyticsLoggingHelper`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/CrashlyticsLoggingHelper.kt); there is no logcat logging framework by design (see [features.md](features.md)).
- **Analytics** — [`AnalyticsHelper`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/AnalyticsHelper.kt) with enum-typed screen/event/parameter names.
- **Runtime permissions** — [`PermissionsHelper`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/PermissionsHelper.kt), a launcher-based helper reporting granted / denied / permanently-denied per request.
- **Edge-to-edge** — every screen is edge-to-edge via `enableEdgeToEdge()` in [`ViewBindingActivity`](../app/src/main/java/com/cube/sprintzerotemplate/lib/generic/ViewBindingActivity.kt); system-bar overlap is handled with the extensions in [`EdgeToEdgeExtensions.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/extensions/EdgeToEdgeExtensions.kt).
- **Theming** — Material 3 (`Theme.Material3.Light.NoActionBar`) with colour and dimension tokens in [`values`](../app/src/main/res/values); see the token rules in [`CLAUDE.md`](../CLAUDE.md).