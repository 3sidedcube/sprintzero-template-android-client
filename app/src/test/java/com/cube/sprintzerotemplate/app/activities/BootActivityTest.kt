package com.cube.sprintzerotemplate.app.activities

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cube.sprintzerotemplate.lib.util.AnalyticsHelper
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric tests for [BootActivity]'s splash flow — the example for testing time-based
 * navigation by advancing the main looper's clock, and for stubbing a Kotlin object
 * (AnalyticsHelper) with mockkObject so no real Firebase is touched
 */
@RunWith(AndroidJUnit4::class)
class BootActivityTest {

	@Before
	fun setUp() {
		mockkObject(AnalyticsHelper)
		justRun { AnalyticsHelper.setupAnalytics(any()) }
	}

	@After
	fun tearDown() {
		unmockkAll()
	}

	@Test
	fun boot_beforeTheSplashDelay_staysOnTheSplash() {
		val activity = Robolectric.buildActivity(BootActivity::class.java).setup().get()

		shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

		assertNull(shadowOf(activity).nextStartedActivity)
	}

	@Test
	fun boot_afterTheSplashDelay_enablesAnalyticsAndOpensTheMainTabs() {
		val activity = Robolectric.buildActivity(BootActivity::class.java).setup().get()

		shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))

		verify { AnalyticsHelper.setupAnalytics(true) }
		assertEquals(MainTabbedActivity::class.java.name, shadowOf(activity).nextStartedActivity?.component?.className)
	}
}