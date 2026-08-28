# Features

A map of what the app actually does, organised by capability rather than by screen. The template deliberately ships no product features — the five tab pages are placeholders — so the capabilities below are the wired-up infrastructure a generated client starts with. The app has no sign-in, so the lifecycle boundary probed here is an app reinstall.

## At a glance

| Feature | State lives in | Survives reinstall? |
|---|---|---|
| [Crash reporting & logging](#crash-reporting--logging) | Firebase Crashlytics (backend) | Yes — server-side |
| [Usage analytics](#usage-analytics) | Firebase Analytics (backend) | Yes — server-side |
| [Push messaging](#push-messaging) | FCM token held by Firebase; no local state | Token is reissued on reinstall |

Everything the template records lives server-side with Firebase; the only device-local state is the `SharedPreferences` store, which currently holds a single example value.

---

## Crash reporting & logging

All logging goes directly to Firebase Crashlytics — there is deliberately no logcat logging framework (Timber was removed for security). The non-obvious mechanic: `logInfo` writes a breadcrumb that is only uploaded attached to the next crash or non-fatal report, never on its own, and `logError` records a non-fatal exception with an optional context breadcrumb. Collection is switched off for debug builds in the application class, so local development sessions never pollute crash data. Release builds are minified but keep `SourceFile`/`LineNumberTable` attributes, and the Crashlytics Gradle plugin uploads the R8 mapping file so reports stay readable.

**Where it lives:** [`CrashlyticsLoggingHelper.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/CrashlyticsLoggingHelper.kt), [`SprintZeroTemplateApp.kt`](../app/src/main/java/com/cube/sprintzerotemplate/app/SprintZeroTemplateApp.kt), [`proguard-rules.pro`](../app/proguard-rules.pro).

## Usage analytics

Firebase Analytics, accessed only through a helper that forces screen names, event names and parameter names through enums so the event vocabulary stays in one place. Collection is enabled unconditionally during boot.

> **TODO:** Wire `setupAnalytics(...)` to a user consent preference — the call in `BootActivity` is hardcoded to `true` and carries a matching TODO in code.

**Where it lives:** [`AnalyticsHelper.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/AnalyticsHelper.kt), [`BootActivity.kt`](../app/src/main/java/com/cube/sprintzerotemplate/app/activities/BootActivity.kt).

## Push messaging

A Firebase Cloud Messaging service is registered in the manifest for `MESSAGING_EVENT`, so the app receives token registrations and data messages — but both callbacks are stubs, so nothing is displayed or persisted until a client implements them. Note that the token callback is `onRegistered` (the current firebase-messaging API), not the deprecated `onNewToken`.

**Where it lives:** [`MessagingService.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/services/MessagingService.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml).

---

## Supporting scaffolding

Not features — the building blocks features are assembled from, listed so you can find them, not because they do anything on their own.

| Piece | What it is | Location |
|---|---|---|
| Boot flow | Splash screen that holds briefly, enables analytics, then opens the tab shell | [`BootActivity.kt`](../app/src/main/java/com/cube/sprintzerotemplate/app/activities/BootActivity.kt) |
| Tabbed shell | Bottom-navigation host for the five placeholder pages, one nav graph per tab | [`MainTabbedActivity.kt`](../app/src/main/java/com/cube/sprintzerotemplate/app/activities/MainTabbedActivity.kt) |
| Placeholder pages | Five identical fragments intended to be replaced by client screens | [`app/fragments`](../app/src/main/java/com/cube/sprintzerotemplate/app/fragments) |
| Base activity | ViewBinding base class that also enables edge-to-edge | [`ViewBindingActivity.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/generic/ViewBindingActivity.kt) |
| Permissions helper | Launcher-based runtime permission requests with granted/denied/permanently-denied results | [`PermissionsHelper.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/PermissionsHelper.kt) |
| Edge-to-edge extensions | System-bar/cutout/IME inset handling for views, hardened against known device quirks | [`EdgeToEdgeExtensions.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/extensions/EdgeToEdgeExtensions.kt) |
| Preferences store | `SharedPreferences` singleton initialised at app start | [`GlobalAppPreferences.kt`](../app/src/main/java/com/cube/sprintzerotemplate/lib/preferences/GlobalAppPreferences.kt) |
| Secrets pattern | Gitignored `secret.properties` with committed example fallback, exposed as `BuildConfig` fields | [`secret-examples.properties`](../secret-examples.properties), [`app/build.gradle.kts`](../app/build.gradle.kts) |