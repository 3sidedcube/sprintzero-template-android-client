package com.cube.sprintzerotemplate.app

import android.app.Application
import com.cube.sprintzerotemplate.BuildConfig
import com.cube.sprintzerotemplate.lib.preferences.GlobalAppPreferences
import com.cube.sprintzerotemplate.lib.util.TimberProductionTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for the app
 */
@HiltAndroidApp
class SprintZeroTemplateApp : Application() {

	override fun onCreate() {
		super.onCreate()

		initialiseLogging()
		initialiseSharedPrefs()
	}

	/**
	 * Set up logging with [Timber]
	 * In production non-error logs are no-ops — see [TimberProductionTree]
	 */
	private fun initialiseLogging() {
		if (!BuildConfig.DEBUG) {
			Timber.plant(TimberProductionTree())
		} else {
			Timber.plant(Timber.DebugTree())
		}
	}

	/**
	 * Initialise all singleton access of shared prefs
	 */
	private fun initialiseSharedPrefs() {
		GlobalAppPreferences.init(applicationContext)
	}
}