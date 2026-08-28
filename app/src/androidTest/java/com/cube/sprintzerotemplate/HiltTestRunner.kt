package com.cube.sprintzerotemplate

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that swaps the real Application for [HiltTestApplication] so
 * @HiltAndroidTest instrumented tests get a fresh Hilt component per test.
 * Wired in as testInstrumentationRunner in app/build.gradle.kts
 */
class HiltTestRunner : AndroidJUnitRunner() {
	override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}