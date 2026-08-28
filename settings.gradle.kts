// The dependencyResolutionManagement repository APIs are still @Incubating (even in Gradle 9.7)
// but are the AGP-recommended setup, so silence the unstable-API warnings deliberately
@file:Suppress("UnstableApiUsage")

pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
	}
}

rootProject.name = "Sprint Zero Template"
include(":app")
