package com.cube.sprintzerotemplate.lib.util

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * MockK tests for [CrashlyticsLoggingHelper] — the example for testing code that talks to
 * Firebase: mock the Firebase.crashlytics accessor statically, never touch the real SDK
 */
class CrashlyticsLoggingHelperTest {
	private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

	@Before
	fun setUp() {
		mockkStatic("com.google.firebase.crashlytics.FirebaseCrashlyticsKt")
		every { Firebase.crashlytics } returns crashlytics
	}

	@After
	fun tearDown() {
		unmockkAll()
	}

	@Test
	fun logInfo_writesBreadcrumb() {
		CrashlyticsLoggingHelper.logInfo("something happened")

		verify(exactly = 1) { crashlytics.log("something happened") }
	}

	@Test
	fun logError_withMessage_logsMessageThenRecordsException() {
		val error = IOException("boom")

		CrashlyticsLoggingHelper.logError(error, "context for the crash")

		verify(exactly = 1) { crashlytics.log("context for the crash") }
		verify(exactly = 1) { crashlytics.recordException(error) }
	}

	@Test
	fun logError_withNullMessage_onlyRecordsException() {
		val error = IOException("boom")

		CrashlyticsLoggingHelper.logError(error, null)

		verify(exactly = 0) { crashlytics.log(any()) }
		verify(exactly = 1) { crashlytics.recordException(error) }
	}

	@Test
	fun logError_withBlankMessage_onlyRecordsException() {
		val error = IOException("boom")

		CrashlyticsLoggingHelper.logError(error, "   ")

		verify(exactly = 0) { crashlytics.log(any()) }
		verify(exactly = 1) { crashlytics.recordException(error) }
	}
}