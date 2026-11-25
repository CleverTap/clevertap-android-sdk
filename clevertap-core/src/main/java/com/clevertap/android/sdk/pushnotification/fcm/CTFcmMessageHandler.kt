package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.sdk.interfaces.IPushAmpHandler
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.google.firebase.messaging.RemoteMessage

/**
 * implementation of [IFcmMessageHandler] and [IPushAmpHandler] for Firebase notification message
 */
open class CTFcmMessageHandler internal constructor(
    private val mParser: INotificationParser<RemoteMessage>
) : IFcmMessageHandler, IPushAmpHandler<RemoteMessage> {

    constructor() : this(FcmNotificationParser())

    /**
     * {@inheritDoc}
     *
     * Use this method if you have custom implementation of messaging service and wants to create push-template
     * notification/non push-template notification using CleverTap
     *
     * **Note: Starting from v5.1.0, this method runs on the caller's thread. Make sure to call it
     * in onMessageReceive() of messaging service.**
     */
    override fun createNotification(applicationContext: Context?, message: RemoteMessage?): Boolean {
        /*
         * Convert firebase message to bundle and pass to PushNotificationHandler for further processing
         */
        var isSuccess = false

        var messageBundle = toBundle(message)
        if (messageBundle != null) {
            /*
             * Analytics: If FCM alters original priority of a notification
             */
            messageBundle = FcmNotificationBundleManipulation(messageBundle).addPriority(message!!).build()

            isSuccess = PushNotificationHandler.getPushNotificationHandler()
                .onMessageReceived(applicationContext, messageBundle, PushConstants.FCM_PUSH_TYPE)
        }

        return isSuccess
    }

    /**
     * {@inheritDoc}
     */
    override fun onNewToken(applicationContext: Context?, token: String?): Boolean {
        var isSuccess = false
        try {
            PushNotificationHandler
                .getPushNotificationHandler()
                .onNewToken(applicationContext, token, PushConstants.FCM)

            Logger.d(LOG_TAG, FCM_LOG_TAG + "New token received from FCM - " + token)
            isSuccess = true
        } catch (t: Throwable) {
            // do nothing
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Error onNewToken", t)
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
        val messageBundle = mParser.toBundle(message)
        if (messageBundle != null) {
            CleverTapAPI.processPushNotification(context, messageBundle)
        }
    }

    /**
     * The parser contract takes a non-null message. The Java implementation handed a null message to
     * the parser, which failed inside its own try/catch, logged and returned `null`; keep that result.
     */
    private fun toBundle(message: RemoteMessage?): Bundle? {
        if (message == null) {
            Logger.d(LOG_TAG, FCM_LOG_TAG + "Invalid Notification Message ", NullPointerException())
            return null
        }
        return mParser.toBundle(message)
    }
}
