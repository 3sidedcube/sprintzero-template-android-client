// Top-level build file. Versions live in gradle/libs.versions.toml.
// AGP 9 provides built-in Kotlin support — org.jetbrains.kotlin.android is intentionally absent.
plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.hilt) apply false
	alias(libs.plugins.google.services) apply false
	alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.register<Delete>("clean") {
	delete(rootProject.layout.buildDirectory)
}

tasks.register<Copy>("installGitHook") {
	from(rootProject.file("scripts/pre-commit"))
	into(rootProject.file(".git/hooks"))
	filePermissions {
		unix("0755")
	}
}

tasks.register<Delete>("removeGitHook") {
	delete(rootProject.file(".git/hooks/pre-commit"))
}
