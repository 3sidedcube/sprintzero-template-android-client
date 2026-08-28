# Security

The security measures this app actually ships today. Every item below is something you can point at in the code — this is a factual record, not a statement of intent.

## Data at rest

Local storage is plain `SharedPreferences` via [`GlobalAppPreferences`](../app/src/main/java/com/cube/sprintzerotemplate/lib/preferences/GlobalAppPreferences.kt); it is not encrypted. There is no database.

## Data in transit

The app has no networking layer, so there is no network security configuration and no certificate pinning. A client adding networking adds those alongside it.

## Authentication & secrets

There is no user authentication. Build-time secrets follow the `secret.properties` pattern: real values live in a gitignored file, the committed [`secret-examples.properties`](../secret-examples.properties) holds placeholders only, and values reach code as `BuildConfig` fields wired in [`app/build.gradle.kts`](../app/build.gradle.kts). The committed per-flavor [`google-services.json`](../app/src/firebaseStaging/google-services.json) files are placeholders; real Firebase configs are committed only to private client repositories by the bootstrap flow.

## Platform hardening

- **Release minification** — `isMinifyEnabled` and `isShrinkResources` are on for release builds ([`app/build.gradle.kts`](../app/build.gradle.kts)); `SourceFile`/`LineNumberTable` are kept for readable Crashlytics traces ([`proguard-rules.pro`](../app/proguard-rules.pro)).
- **Backup** — `android:allowBackup` is `true` with the stock, empty backup and data-extraction rule files ([`backup_rules.xml`](../app/src/main/res/xml/backup_rules.xml), [`data_extraction_rules.xml`](../app/src/main/res/xml/data_extraction_rules.xml)), so all app data participates in Auto Backup by default. Clients storing sensitive data must scope these rules.
- **Exported components** — [`BootActivity`](../app/src/main/AndroidManifest.xml) is exported as the launcher; `MainTabbedActivity` is also declared `exported="true"` despite having no intent filter. `MessagingService` is not exported.
- **Permissions** — the manifest declares no permissions; [`PermissionsHelper`](../app/src/main/java/com/cube/sprintzerotemplate/lib/util/PermissionsHelper.kt) exists for clients that add them.
- **Crash-data hygiene** — Crashlytics collection is disabled for debug builds in [`SprintZeroTemplateApp`](../app/src/main/java/com/cube/sprintzerotemplate/app/SprintZeroTemplateApp.kt), and there is deliberately no device-log framework (Timber was removed so logs cannot leak via logcat).

## Supply chain

Shipping compile/runtime classpaths are dependency-locked ([`app/gradle.lockfile`](../app/gradle.lockfile), [`settings-gradle.lockfile`](../settings-gradle.lockfile)) and Dependabot raises weekly grouped update PRs ([`.github/dependabot.yml`](../.github/dependabot.yml)). There is no secret-scanning or dependency-review CI step; those were removed as team decisions.