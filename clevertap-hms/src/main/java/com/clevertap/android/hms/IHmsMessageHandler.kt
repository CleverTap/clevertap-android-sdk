package com.clevertap.android.hms

import android.content.Context
import androidx.annotation.RestrictTo
import com.huawei.hms.push.RemoteMessage

/**
 * interface to handle the huawei notification message service callbacks
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IHmsMessageHandler {

    /**
     * Creates notification from Huawei Remote message
     * @param context       - application context
     * @param remoteMessage - Huawei Remote Message
     * @return true if everything is fine & notification is rendered successfully
     */
    fun createNotification(context: Context?, remoteMessage: RemoteMessage?): Boolean

    /**
     * Processes new token from Huawei
     * @param context - application context
     * @param token   - fcm token received from Huawei SDK
     * @return true if the token is sent to Clevertap's server
     */
    fun onNewToken(context: Context?, token: String?): Boolean
}
