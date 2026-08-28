package com.cube.sprintzerotemplate.lib.services

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Messaging Service to handle push notification from Firebase
 * TODO Stub Needs Implementing Fully
 */
// Lint still asks for onNewToken, which firebase-messaging 25.1 deprecated in favour of onRegistered
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MessagingService : FirebaseMessagingService() {
	override fun onRegistered(token: String) {
		super.onRegistered(token)
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		super.onMessageReceived(remoteMessage)
	}
}