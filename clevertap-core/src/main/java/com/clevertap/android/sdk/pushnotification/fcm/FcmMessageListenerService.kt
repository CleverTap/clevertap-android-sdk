package com.clevertap.android.sdk.pushnotification.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Clevertap's Implementation for Firebase Message service
 */
open class FcmMessageListenerService : FirebaseMessagingService() {

    private val mHandler: IFcmMessageHandler = CTFcmMessageHandler()

    override fun onMessageReceived(message: RemoteMessage) {
        mHandler.createNotification(applicationContext, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        mHandler.onNewToken(applicationContext, token)
    }
}
