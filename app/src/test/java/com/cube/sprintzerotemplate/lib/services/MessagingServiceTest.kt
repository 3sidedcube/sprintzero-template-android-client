package com.cube.sprintzerotemplate.lib.services

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * Smoke test for the [MessagingService] stub — grows with the real push handling.
 * TODO Expand alongside the TODO in MessagingService itself
 */
@RunWith(AndroidJUnit4::class)
class MessagingServiceTest {

	@Test
	fun serviceCallbacks_stubImplementations_doNotCrash() {
		val service = Robolectric.buildService(MessagingService::class.java).create().get()

		service.onRegistered("a-new-fcm-token")
		service.onMessageReceived(RemoteMessage.Builder("test@fcm.googleapis.com").build())
	}
}