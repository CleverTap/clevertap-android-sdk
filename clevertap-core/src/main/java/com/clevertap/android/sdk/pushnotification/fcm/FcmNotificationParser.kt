package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.google.firebase.messaging.RemoteMessage

/**
 * Implementation of [INotificationParser]<[RemoteMessage]>
 */
internal class FcmNotificationParser : INotificationParser<RemoteMessage> {

    override fun toBundle(message: RemoteMessage): Bundle? {
        try {
            val extras = Bundle()
            for ((key, value) in message.data) {
                extras.putString(key, value)
            }
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Found Valid Notification Message ")
            return extras
        } catch (e: Throwable) {
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Invalid Notification Message ", e)
        }
        return null
    }
}
