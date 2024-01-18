package com.cube.sprintzerotemplate.lib.util

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * Production logging tree to be used by [Timber]
 *
 * @author Kieran Hawkins
 */
class TimberProductionTree : Timber.Tree() {
	override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
		if (priority == Log.ERROR && t != null) {
			tag?.let {
				Firebase.crashlytics.log(it)
			}
			if (message.isNotBlank()) {
				Firebase.crashlytics.log(message)
			}
			Firebase.crashlytics.recordException(t)
		}
		// If not an error, do not log at all in production
	}
}