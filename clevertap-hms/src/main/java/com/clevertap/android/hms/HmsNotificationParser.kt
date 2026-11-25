package com.clevertap.android.hms

import android.os.Bundle
import com.clevertap.android.hms.HmsConstants.Companion.HMS_LOG_TAG
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.huawei.hms.push.RemoteMessage

/**
 * Implementation of [INotificationParser]<[RemoteMessage]>
 */
internal class HmsNotificationParser : INotificationParser<RemoteMessage> {

    override fun toBundle(message: RemoteMessage): Bundle? {
        try {
            val extras = Utils.stringToBundle(message.data)
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Found Valid Notification Message ")
            return extras
        } catch (e: Throwable) {
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Invalid Notification Message ", e)
        }
        return null
    }
}
