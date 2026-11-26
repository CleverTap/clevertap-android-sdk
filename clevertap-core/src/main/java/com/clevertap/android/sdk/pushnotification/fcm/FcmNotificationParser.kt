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

    override fun toBundle(message: RemoteMessage): Bundle? = try {
        Bundle().apply {
            for ((key, value) in message.data) {
                putString(key, value)
            }
        }.also { Logger.d(LOG_TAG, "${FCM_LOG_TAG}Found Valid Notification Message ") }
    } catch (e: Throwable) {
        Logger.d(LOG_TAG, "${FCM_LOG_TAG}Invalid Notification Message ", e)
        null
    }
}
