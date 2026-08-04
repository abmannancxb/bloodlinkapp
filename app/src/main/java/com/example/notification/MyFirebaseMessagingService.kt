package com.example.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract push notification details
        val data = remoteMessage.data
        val bloodGroup = data["bloodGroup"] ?: "O+"
        val location = data["location"] ?: "Ramu"
        val condition = data["condition"] ?: "Emergency"

        // Display local push notification
        LocalNotificationHelper.showUrgentRequestNotification(
            applicationContext,
            bloodGroup,
            location,
            condition
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server/Firestore if needed
    }
}
