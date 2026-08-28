import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.google.services)
	alias(libs.plugins.firebase.crashlytics)
}

// Secrets: copy secret-examples.properties to secret.properties (gitignored) and fill in
// real values. Fresh clones and CI fall back to the example placeholders so the build
// stays green; wire each secret in as a buildConfigField below.
val secrets = Properties().apply {
	val secretsFile = rootProject.file("secret.properties").takeIf { it.exists() }
		?: rootProject.file("secret-examples.properties")
	secretsFile.inputStream().use { load(it) }
}

android {
	namespace = "com.cube.sprintzerotemplate"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.cube.sprintzerotemplate"
		minSdk = 30
		targetSdk = 37
		versionCode = 1
		versionName = "0.1.0"

		testInstrumentationRunner = "com.cube.sprintzerotemplate.HiltTestRunner"
		vectorDrawables {
			useSupportLibrary = true
		}

		buildConfigField("String", "EXAMPLE_API_KEY", "\"${secrets.getProperty("EXAMPLE_API_KEY")}\"")
	}

	flavorDimensions += listOf("firebase", "api")
	productFlavors {
		create("firebaseDev") {
			dimension = "firebase"
		}
		create("firebaseStaging") {
			dimension = "firebase"
		}
		create("firebaseLive") {
			dimension = "firebase"
		}
		create("apiDev") {
			dimension = "api"
			buildConfigField("String", "API_URL", "\"https://api.dev.goes.here.com\"")
		}
		create("apiStaging") {
			dimension = "api"
			buildConfigField("String", "API_URL", "\"https://api.staging.goes.here.com\"")
		}
		create("apiLive") {
			dimension = "api"
			buildConfigField("String", "API_URL", "\"https://api.live.goes.here.com\"")
		}
	}

	buildTypes {
		debug {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		viewBinding = true
		buildConfig = true
	}

	testOptions {
		unitTests {
			// Robolectric tests inflate real layouts and read real resources
			isIncludeAndroidResources = true
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

// Dependency locking for OSV scanning. Lock only the shipping classpaths —
// locking everything also locks per-variant AGP/KSP processor classpaths,
// which resolve to nothing and bloat the lockfile past osv-scanner's limits.
// Regenerate with: ./gradlew :app:dependencies --write-locks
val ktlint = configurations.create("ktlint")

configurations.matching {
	val isAppClasspath = it.name.endsWith("RuntimeClasspath") || it.name.endsWith("CompileClasspath")
	val isProcessorClasspath = it.name.startsWith("_agp_internal") ||
		it.name.endsWith("kaptClasspath") || it.name.endsWith("kspClasspath")
	val isTestOrLintClasspath = it.name.contains("UnitTest") ||
		it.name.contains("AndroidTest") || it.name.contains("LintChecks")
	isAppClasspath && !isProcessorClasspath && !isTestOrLintClasspath
}.configureEach {
	resolutionStrategy.activateDependencyLocking()
}

dependencies {
	// Core
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.activity.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.fragment.ktx)

	// UI
	implementation(libs.material)
	implementation(libs.lottie)

	// Dependency injection
	implementation(libs.hilt.android)
	ksp(libs.dagger.compiler)
	ksp(libs.hilt.compiler)

	// Navigation
	implementation(libs.androidx.navigation.fragment.ktx)
	implementation(libs.androidx.navigation.ui.ktx)

	// Splash screen
	implementation(libs.androidx.core.splashscreen)

	// Firebase (BoM-managed — no versions on individual artifacts)
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.analytics)
	implementation(libs.firebase.crashlytics)
	implementation(libs.firebase.messaging)

	// Unit tests (JVM) — Robolectric for Android-dependent code, MockK for mocking
	testImplementation(libs.junit)
	testImplementation(libs.androidx.junit)
	testImplementation(libs.androidx.test.core.ktx)
	testImplementation(libs.mockk)
	testImplementation(libs.robolectric)
	testImplementation(libs.hilt.android.testing)
	kspTest(libs.hilt.compiler)

	// Instrumented tests (device/emulator) — Espresso driven through the HiltTestRunner
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.hilt.android.testing)
	kspAndroidTest(libs.hilt.compiler)

	// ktlint CLI (runs via the ktlint/ktlintFormat tasks below)
	ktlint("com.pinterest.ktlint:ktlint-cli:${libs.versions.ktlintCli.get()}") {
		attributes {
			attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
		}
	}
}

tasks.register<JavaExec>("ktlint") {
	group = "verification"
	description = "Check Kotlin code style."
	classpath = ktlint
	mainClass = "com.pinterest.ktlint.Main"
	jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
	// see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
	args("src/**/*.kt", "**.kts", "!**/build/**")
}

tasks.named("check") {
	dependsOn("ktlint")
}

tasks.register<JavaExec>("ktlintFormat") {
	group = "formatting"
	description = "Fix Kotlin code style deviations."
	classpath = ktlint
	mainClass = "com.pinterest.ktlint.Main"
	jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
	// see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
	args("-F", "src/**/*.kt", "**.kts", "!**/build/**")
}