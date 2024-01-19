# Template for Android

This repository provides a clean, well-organised template designed to speed up the process of setting up new projects at Cube. Use it as a starting point for your new Android project, and accelerate your progress by focusing on your app's unique features rather than boilerplate setup.

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

## Creating a New Project at Cube
The following is a step-by-step process for creating a new project at Cube.

### 1. Get all required information:
- Pick a name for your repository. It should be something along the lines of `myproject-android-client`.
- Pick the package name for the Android application. It should be something along the lines of `com.cube.myproject`.
- Pick the project name for the application. It should be something along the lines of `MyProject`.
- If you are not sure of these, consult the project's PM or the Android team.

### 2. Create a new Bitbucket repository:
- Go to Bitbucket and log in.
- Ensure your in the 3 Sided Cube workspace
- Navigate to the repositories section.
- Create a new repository for your project, specifying details such as name (chosen in step 1) and description.
- Be sure to include a new `README.md` and `.gitignore`.
- The main branch should be set to `master`.
- Clone your new repository onto your local machine.
- Navigate into the new repository
- Checkout the master branch using `git checkout master`

### 3. Create a new Firebase project:
- Go to the Firebase console and sign in with the 3 Sided Cube Google account.
- Click on `Add Project` to create a new Firebase project.
- Follow the setup wizard, providing a project name, selecting your preferred analytics settings, and agreeing to the terms.
- Once the project is created, you'll have access to the Firebase dashboard and configuration settings.
- Create a new Android application in the dashboard, providing a package name you have chosen in step 1
- Keep the `google-services.json` for the next step.

### 4. Create a new Sentry project:
- Go to the Sentry console and sign in with the 3 Sided Cube Google account.
- Click on `Create Project` to create a new Sentry project.
- Ensure you are creating an Android project and select to setup your own alerts later.
- Once created, select `Manual` in the tabs and copy the value for `io.sentry.dsn`. You will need this to connect to Sentry later.

```xml
<meta-data android:name="io.sentry.dsn" android:value="https://85b539e94acafada13bf8d86ed2ca1da@o4506592770588672.ingest.sentry.io/4506593210007552" />
```

### 5. Copy build-new-project.rb and google-services.json:
- Locate the `build-new-project.rb` script and copy it into your new repository directory.
- Obtain the `google-services.json` file from your Firebase project settings and copy it into your new repository directory.

### 6. Build your project:
- Open a terminal or command prompt and navigate to your repository directory.
- Run the build process using the configured build script (`ruby build-new-project.rb -p com/cube/myproject -n MyProject -g ./google-services.json`).
- `-p` or `--package-name` is required and should be the application package name chosen in step 1. The format should be `com/cube/myproject`.
- `-n` or `--project-name` is required and should be the application project name. The format should be `MyProject`.
- `-g` or `--gs-path` is required and should be the path to your acquired `google-services.json` file. The format should be `./google-services.json`.
- Adds a new local `gradle.properties` file with the following values. This file is included in the `.gitignore` and will not be committed.

```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
systemProp.SENTRY_DSN={{Your Sentry URL Acquired in Step 4}}
```

- Ensure that the build process completes successfully without errors.
- If everything is okay, you can now make your first commit to the repository to push up the new project.

### 7. Create a new Bitrise project:
- Open the Bitrise dashboard and log in. Ensure your in 3 Sided Cube's workspace.
- Select `Add New App` and select your newly created project repository as the source.
- When asked to authorise Bitrise, select `Auto-add a generated SSH key to your repository`.
- When asked to select a branch, choose `master`.
- When asked to specify the module, keep the default `app` in place.
- There is no need to add a variant, app icon or webhook at this stage so these steps can be skipped.
- You should now be able to view the Bitrise dashboard for your new application.
- In the dashboard, select `Workflows` in the top right.
- Select `Edit bitrise.yml`.
- Select `Store in app repository`.
- Select `Update settings`.
- Your app should now have the correct workflows for building Android apps.

### 8. App signing:
- Create a key store password using 1Password. 
- Create a key store alias using 1Password.
- To generate a keystore, enter the following command into the terminal being sure to replace {{key store name}}, {{key store password}} and {{key store alias password}} with the ones created previously.

```shell
$ keytool -genkey -v -keystore {{key store name}}.keystore -storepass {{key store password}} -alias myproject-alias -keypass {{key store alias password}} -keyalg RSA -keysize 2048 -validity 10000
```

- Add the generated keystore file to 1Password.
- Go back into the Bitrise dashboard for your project.
- In the dashboard, select `Workflows` in the top right.
- Select `Edit bitrise.yml`.
- Select `Code Signing & Files`.
- Add your new keystore file, the alias, keystore password and alias password.
- Your new application should now be able to be built and signed via Bitrise.

### 9. Ready to develop:
- With the Bitbucket repository, Firebase project, build scripts, and Bitrise configuration in place, your project is ready for development.
- Check the Bitrise pipeline runs successfully.
- Your new APK's and AAB's can be downloaded from Bitrise.
- Create a new develop branch within your repository using `git checkout -b develop`
- Ensure you commit all new changes onto separate branches and merge into `develop`
