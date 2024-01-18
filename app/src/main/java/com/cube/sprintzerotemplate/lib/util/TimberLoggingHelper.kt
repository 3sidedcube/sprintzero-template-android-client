package com.cube.sprintzerotemplate.lib.util

import timber.log.Timber

/**
 * Includes data relevant to timber logging
 */
object TimberLoggingHelper {

	/**
	 * Log Messages TODO Add your own hardcoded messages
	 */
	const val MSG_APP_STARTED: String = "App Started"

	/**
	 * Method to log a provided message
	 *
	 * @param message The message to log
	 */
	fun logInfo(message: String) {
		Timber.i(message)
	}

	/**
	 * Method to log a provided error and message
	 *
	 * @param error The error to log
	 * @param message The message to log
	 */
	fun logError(error: Throwable, message: String?) {
		Timber.e(error, message)
	}
}