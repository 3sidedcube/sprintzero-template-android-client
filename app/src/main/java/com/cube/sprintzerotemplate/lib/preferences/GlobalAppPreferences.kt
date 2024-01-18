package com.cube.sprintzerotemplate.lib.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Class providing singleton access to user shared preference values that are global to the application
 *
 * TODO Add your own shared preferences
 *
 * @author Kieran Hawkins
 */
object GlobalAppPreferences {
	private const val PREFERENCES_NAME: String = "GLOBAL_APP_RPREFERENCES"
	lateinit var preferences: SharedPreferences

	/**
	 * Initialise the singleton instance
	 * Should be called in the application onCreate
	 *
	 * @param context the context to instantiate the shared preferences from
	 */
	fun init(context: Context) {
		preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
	}

	/**
	 * Clear all stored shared preferences
	 */
	fun clearAll() {
		preferences.edit().clear().apply()
	}

	/**
	 * Preference key for [testPreference]
	 */
	private const val KEY_TEST_PREFERENCE = "key_test_preference"

	/**
	 * A test preference to be replaced when needed
	 */
	var testPreference: String?
		get() = preferences.getString(KEY_TEST_PREFERENCE, null)
		set(value) = preferences.edit().putString(KEY_TEST_PREFERENCE, value).apply()
}