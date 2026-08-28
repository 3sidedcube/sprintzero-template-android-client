package com.cube.sprintzerotemplate.lib.util

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

/**
 * Logs messages and errors to Crashlytics
 */
object CrashlyticsLoggingHelper {

	/**
	 * Log Messages TODO Add your own hardcoded messages
	 */
	const val MSG_APP_STARTED: String = "App Started"

	/**
	 * Method to log a provided message
	 * Logged messages are attached to the next crash or non-fatal report — they are not sent on their own
	 *
	 * @param message The message to log
	 */
	fun logInfo(message: String) {
		Firebase.crashlytics.log(message)
	}

	/**
	 * Method to record a provided error and message as a non-fatal in Crashlytics
	 *
	 * @param error The error to log
	 * @param message The message to log
	 */
	fun logError(error: Throwable, message: String?) {
		Firebase.crashlytics.run {
			message?.takeIf { it.isNotBlank() }?.let { log(it) }
			recordException(error)
		}
	}
}