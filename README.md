# Template for Android

This repository provides a clean, well-organised template designed to speed up the process of setting up new projects at Cube. Use it as a starting point for your new Android project, and accelerate your progress by focusing on your app's unique features rather than boilerplate setup.

## Gradle Setup
- Adds a new local `gradle.properties` file with the following values. This file is included in the `.gitignore` and will not be committed.

```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
systemProp.SENTRY_DSN={{Your Sentry URL}}
```

## Code formatting
This repo is set up with automatic git commit hooks to ensure well formatted code - credit to Ali Rezaee for the set-up.
After cloning the repository, you should set up the hook with:

```shell
./gradlew installGitHook
```

Or by running the `installGitHook` task from Android Studio's Gradle tab or the `build.gradle` file.

The purpose of this hook is to run the `ktlint` task before it lets you commit.
If it is successful, then it commits successfully, otherwise, it will throw an error explaining the problem.

If the problem is code formatting, you can use the `ktlintFormat` task to automatically format the code (although there are some changes that `ktlintFormat` cannot automatically make, which you will have to manually fix).

One thing to note is that this git hook can cause issues when merging branches, throwing unnecessary errors on the merge commit.
You can temporarily bypass the hook by running the `removeGitHook` task, then performing the merge, and then running `installGitHook` again so that the hook still applies to subsequent commits.

## Repo structure
This repo uses single-module structure, as there is no need to split into multiple modules.
However, the packages are organised similarly to a two-module structure, in order to cleanly separate interests for the code.

The `app` package contains the main application, as well as a package for any distinct element of the app's UI flow.
These subpackages may contain activities, fragments, adapters etc., but should only contain code that is used exclusively in the specified part of the app.

The `lib` package can contain any logic that is not related to UI (e.g config, API logic), or any generic UI logic that is used in multiple parts of the app (e.g views, viewholders).
This way, generic logic can be set up within `lib`, and then utilised in a specific way to achieve the intended UI in `app`.

## Firebase integration
The template uses Firebase for push notifications, analytics and crash reporting. Additional projects can be created as required. The project can be found under the 3SidedCube account

## Timber Logging
The template has Timber logging installed and setup. See [TimberLoggingHelper.kt](app/src/main/java/com/cube/sprintzerotemplate/lib/util/TimberLoggingHelper.kt) and modify as needed.

## Sentry
In production environments, the template is setup to send exception errors and crashes to Sentry. Sentry has been fully integrated with Timber so no additional functions are required.