package com.cube.sprintzerotemplate.app

import android.app.Application
import com.cube.sprintzerotemplate.BuildConfig
import com.cube.sprintzerotemplate.lib.preferences.GlobalAppPreferences
import com.cube.sprintzerotemplate.lib.util.TimberProductionTree
import dagger.hilt.android.HiltAndroidApp
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
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
	 * Set up logging with [SentryAndroid] and [Timber]
	 * Initialised in such a way that we can log anywhere we like but in production all non error logs will be no-ops
	 * Also wraps error logs so that they log to Firebase
	 */
	private fun initialiseLogging() {
		SentryAndroid.init(this) { options ->
			options.dsn = BuildConfig.SENTRY_DSN

			// Integrate Timber
			if (!BuildConfig.DEBUG) {
				Timber.plant(TimberProductionTree())
				options.addIntegration(
					SentryTimberIntegration(
						minEventLevel = SentryLevel.ERROR,
						minBreadcrumbLevel = SentryLevel.INFO
					)
				)
			} else {
				Timber.plant(Timber.DebugTree())
			}

			// With this callback, you can modify the event or, when returning null, also discard the event.
			options.beforeSend = SentryOptions.BeforeSendCallback { event: SentryEvent, _ ->
				if (!BuildConfig.DEBUG) {
					event
				} else {
					null
				}
			}
		}
	}

	/**
	 * Initialise all singleton access of shared prefs
	 */
	private fun initialiseSharedPrefs() {
		GlobalAppPreferences.init(applicationContext)
	}
}