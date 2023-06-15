package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.google.firebase.messaging.RemoteMessage

class FcmNotificationBundleManipulation(private val messageBundle: Bundle) :
    INotificationBundleManipulation<RemoteMessage> {

    override fun addPriority(message: RemoteMessage): INotificationBundleManipulation<RemoteMessage> {
        if (message.originalPriority != message.priority) {
            val strPriority = when (message.priority) {
                RemoteMessage.PRIORITY_HIGH -> Constants.PRIORITY_HIGH
                RemoteMessage.PRIORITY_NORMAL -> Constants.PRIORITY_NORMAL
                RemoteMessage.PRIORITY_UNKNOWN -> Constants.PRIORITY_UNKNOWN
                else -> ""
            }
            messageBundle.putString(Constants.WZRK_PN_PRT, strPriority)
        }
        return this
    }

    override fun build(): Bundle = messageBundle
}