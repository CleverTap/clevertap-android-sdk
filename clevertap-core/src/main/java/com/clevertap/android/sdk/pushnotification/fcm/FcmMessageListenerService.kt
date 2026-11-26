package com.clevertap.android.sdk.pushnotification.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Clevertap's Implementation for Firebase Message service
 */
public open class FcmMessageListenerService : FirebaseMessagingService() {

    private val handler: IFcmMessageHandler = CTFcmMessageHandler()

    override fun onMessageReceived(message: RemoteMessage) {
        handler.createNotification(applicationContext, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        handler.onNewToken(applicationContext, token)
    }
}
