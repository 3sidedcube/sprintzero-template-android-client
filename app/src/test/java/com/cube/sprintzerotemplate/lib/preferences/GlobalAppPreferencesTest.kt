package com.cube.sprintzerotemplate.lib.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests for [GlobalAppPreferences] — the example for testing SharedPreferences-backed
 * singletons against a real Android context on the JVM
 */
@RunWith(AndroidJUnit4::class)
class GlobalAppPreferencesTest {

	@Before
	fun setUp() {
		GlobalAppPreferences.init(ApplicationProvider.getApplicationContext())
		GlobalAppPreferences.clearAll()
	}

	@Test
	fun testPreference_unset_returnsNull() {
		assertNull(GlobalAppPreferences.testPreference)
	}

	@Test
	fun testPreference_set_roundTrips() {
		GlobalAppPreferences.testPreference = "stored value"

		assertEquals("stored value", GlobalAppPreferences.testPreference)
	}

	@Test
	fun testPreference_setNull_clearsTheValue() {
		GlobalAppPreferences.testPreference = "stored value"

		GlobalAppPreferences.testPreference = null

		assertNull(GlobalAppPreferences.testPreference)
	}

	@Test
	fun clearAll_removesStoredValues() {
		GlobalAppPreferences.testPreference = "stored value"

		GlobalAppPreferences.clearAll()

		assertNull(GlobalAppPreferences.testPreference)
	}
}