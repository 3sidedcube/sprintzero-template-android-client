package com.cube.sprintzerotemplate.lib.util

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [AnalyticsHelper]. Robolectric supplies a real [Bundle]; the FirebaseAnalytics
 * instance behind the lazy Firebase.analytics accessor is a MockK static mock.
 *
 * The static mock is installed once for the JVM (not un-mocked per test) because
 * [AnalyticsHelper] caches the instance in a lazy — un-mocking would leave the object
 * holding a stale mock for any other test class that touches it
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsHelperTest {

	@Before
	fun setUp() {
		mockkStatic("com.google.firebase.analytics.AnalyticsKt")
		every { Firebase.analytics } returns firebaseAnalytics
		// The mock is shared across tests (see companion) — drop calls recorded by earlier tests
		clearMocks(firebaseAnalytics, answers = false)
	}

	@Test
	fun setupAnalytics_togglesCollection() {
		AnalyticsHelper.setupAnalytics(true)
		verify { firebaseAnalytics.setAnalyticsCollectionEnabled(true) }

		AnalyticsHelper.setupAnalytics(false)
		verify { firebaseAnalytics.setAnalyticsCollectionEnabled(false) }
	}

	@Test
	fun screenView_logsScreenViewEventWithScreenName() {
		val bundle = slot<Bundle>()

		AnalyticsHelper.screenView(AnalyticsHelper.ScreenViews.APP_LANDING)

		verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, capture(bundle)) }
		assertEquals("App landing page", bundle.captured.getString(FirebaseAnalytics.Param.SCREEN_NAME))
	}

	@Test
	fun screenView_withCustomTitle_appendsItToTheScreenName() {
		val bundle = slot<Bundle>()

		AnalyticsHelper.screenView(AnalyticsHelper.ScreenViews.APP_LANDING, " - detail")

		verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, capture(bundle)) }
		assertEquals("App landing page - detail", bundle.captured.getString(FirebaseAnalytics.Param.SCREEN_NAME))
	}

	@Test
	fun sendEvent_logsEventWithNonEmptyParamsOnly() {
		val bundle = slot<Bundle>()

		AnalyticsHelper.sendEvent(
			AnalyticsHelper.EventNames.APP_START,
			AnalyticsHelper.ParamNames.APP_LOCATION to "home",
			AnalyticsHelper.ParamNames.APP_LOCATION to ""
		)

		verify { firebaseAnalytics.logEvent("app_start", capture(bundle)) }
		assertEquals("home", bundle.captured.getString("location"))
		assertEquals(1, bundle.captured.size())
	}

	companion object {
		/**
		 * Shared across tests because [AnalyticsHelper]'s lazy caches whichever instance the
		 * first access resolves
		 */
		private val firebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)
	}
}