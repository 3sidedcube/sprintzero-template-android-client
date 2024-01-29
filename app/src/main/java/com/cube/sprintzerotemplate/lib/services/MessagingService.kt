package com.cube.sprintzerotemplate.lib.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Messaging Service to handle push notification from Firebase
 * TODO Stub Needs Implementing Fully
 */
class MessagingService : FirebaseMessagingService() {
	override fun onNewToken(token: String) {
		super.onNewToken(token)
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		super.onMessageReceived(remoteMessage)
	}
}