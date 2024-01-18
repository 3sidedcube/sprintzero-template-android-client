package com.cube.sprintzerotemplate.lib.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * This class include the functions we're going to use for analytics
 */
object AnalyticsHelper {
	private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

	/**
	 * Method to enable/disable analytics based on preference
	 */
	fun setupAnalytics(enabled: Boolean) {
		firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
	}

	/**
	 * Method to log a screen view event
	 */
	fun screenView(screenName: ScreenViews, customTitle: String? = null) {
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
			param(FirebaseAnalytics.Param.SCREEN_NAME, screenName.title + (customTitle ?: ""))
		}
	}

	/**
	 * Method to log a custom event
	 */
	fun sendEvent(eventName: EventNames, vararg params: Pair<ParamNames, String>) {
		firebaseAnalytics.logEvent(eventName.title) {
			params.forEach { (name, value) ->
				if (value.isNotEmpty()) {
					param(name.title, value)
				}
			}
		}
	}

	/**
	 * Enum class for screen view names
	 */
	enum class ScreenViews(val title: String) {
		APP_LANDING("App landing page")
	}

	/**
	 * Enum class for event names
	 */
	enum class EventNames(val title: String) {
		APP_START("app_start")
	}

	/**
	 * Enum class for event parameter names
	 */
	enum class ParamNames(val title: String) {
		APP_LOCATION("location")
	}
}