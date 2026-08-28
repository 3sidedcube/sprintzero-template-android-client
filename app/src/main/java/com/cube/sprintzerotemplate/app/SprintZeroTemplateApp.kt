package com.cube.sprintzerotemplate.app

import android.app.Application
import com.cube.sprintzerotemplate.BuildConfig
import com.cube.sprintzerotemplate.lib.preferences.GlobalAppPreferences
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the app
 */
@HiltAndroidApp
class SprintZeroTemplateApp : Application() {

	override fun onCreate() {
		super.onCreate()

		initialiseCrashlytics()
		initialiseSharedPrefs()
	}

	/**
	 * Only report to Crashlytics from release builds so debug sessions don't pollute crash data
	 */
	private fun initialiseCrashlytics() {
		Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
	}

	/**
	 * Initialise all singleton access of shared prefs
	 */
	private fun initialiseSharedPrefs() {
		GlobalAppPreferences.init(applicationContext)
	}
}