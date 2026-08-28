import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.google.services)
	alias(libs.plugins.firebase.crashlytics)
}

apply(from = "../ktlint.gradle")

android {
	namespace = "com.cube.sprintzerotemplate"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.cube.sprintzerotemplate"
		minSdk = 30
		targetSdk = 37
		versionCode = 1
		versionName = "0.1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	flavorDimensions += listOf("firebase", "api")
	productFlavors {
		create("firebaseStaging") {
			dimension = "firebase"
		}
		create("firebaseLive") {
			dimension = "firebase"
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
			isMinifyEnabled = false
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

	// Tests
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}