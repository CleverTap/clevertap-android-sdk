package com.clevertap.android.hms

import android.content.Context
import com.clevertap.android.hms.HmsConstants.Companion.HMS_LOG_TAG
import com.clevertap.android.hms.HmsConstants.Companion.HPS
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.sdk.interfaces.IPushAmpHandler
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.clevertap.android.sdk.pushnotification.fcm.IFcmMessageHandler
import com.huawei.hms.push.RemoteMessage

/**
 * implementation of [IFcmMessageHandler] and [IPushAmpHandler] for huawei notification message
 */
public open class CTHmsMessageHandler internal constructor(
    private val mParser: INotificationParser<RemoteMessage>
) : IHmsMessageHandler, IPushAmpHandler<RemoteMessage> {

    public constructor() : this(HmsNotificationParser())

    /**
     * {@inheritDoc}
     *
     * Use this method if you have custom implementation of huawei messaging service and wants to create push-template
     * notification/non push-template notification using CleverTap
     *
     * **Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
     * in onMessageReceive() of messaging service.**
     */
    override fun createNotification(context: Context?, remoteMessage: RemoteMessage?): Boolean {
        var isSuccess = false
        // The parser contract is @NonNull; Java passed null through and the parser's own catch
        // turned the resulting NPE into a null bundle. Skip the call instead (same outcome: false).
        val messageBundle = if (remoteMessage != null) { mParser.toBundle(remoteMessage) } else { null }
        if (messageBundle != null) {
            try {
                isSuccess = PushNotificationHandler
                    .getPushNotificationHandler().onMessageReceived(context, messageBundle, HPS.type)
            } catch (e: Throwable) {
                Logger.d(LOG_TAG, HMS_LOG_TAG + "Error Creating Notification", e)
            }
        }
        return isSuccess
    }

    /**
     * {@inheritDoc}
     */
    override fun onNewToken(context: Context?, token: String?): Boolean {
        var isSuccess = false
        try {
            PushNotificationHandler
                .getPushNotificationHandler()
                .onNewToken(context, token, HPS)
            Logger.d(LOG_TAG, HMS_LOG_TAG + "onNewToken: " + token)
            isSuccess = true
        } catch (throwable: Throwable) {
            // do nothing
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Error onNewToken: " + token, throwable)
        }
        return isSuccess
    }

    /**
     * {@inheritDoc}
     *
     * Use this method if you are rendering notification by your own and wants to support your custom rendered
     * notification for Pull Notifications
     */
    override fun processPushAmp(context: Context?, message: RemoteMessage) {
        try {
            val messageBundle = mParser.toBundle(message)
            if (messageBundle != null) {
                CleverTapAPI.processPushNotification(context, messageBundle)
            }
        } catch (t: Throwable) {
            Logger.d(LOG_TAG, HMS_LOG_TAG + "Error processing push amp", t)
        }
    }
}
