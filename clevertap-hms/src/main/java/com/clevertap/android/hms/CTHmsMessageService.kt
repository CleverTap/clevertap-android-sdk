package com.clevertap.android.hms

import com.clevertap.android.hms.HmsConstants.Companion.HMS_LOG_TAG
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

/**
 * Clevertap's Implementation for Huawei Message service
 */
internal class CTHmsMessageService : HmsMessageService() {

    private val mHandler: IHmsMessageHandler = CTHmsMessageHandler(HmsNotificationParser())

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Logger.i(LOG_TAG, HMS_LOG_TAG + "onMessageReceived is called")
        mHandler.createNotification(applicationContext, remoteMessage)
    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Logger.i(LOG_TAG, HMS_LOG_TAG + "onNewToken is called " + token)
        mHandler.onNewToken(applicationContext, token)
    }
}
