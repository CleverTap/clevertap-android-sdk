package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Context
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
public open class CTFcmMessageHandler internal constructor(
    private val parser: INotificationParser<RemoteMessage>
) : IFcmMessageHandler, IPushAmpHandler<RemoteMessage> {

    public constructor() : this(FcmNotificationParser())

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
        if (message == null) {
            // The parser contract takes a non-null message. The Java implementation handed a null
            // message to the parser, which failed inside its own try/catch, logged and returned null.
            Logger.d(LOG_TAG, "${FCM_LOG_TAG}Invalid Notification Message ", NullPointerException())
            return false
        }
        val messageBundle = parser.toBundle(message) ?: return false
        /*
         * Analytics: If FCM alters original priority of a notification
         */
        val prioritisedBundle = FcmNotificationBundleManipulation(messageBundle).addPriority(message).build()

        return PushNotificationHandler.getPushNotificationHandler()
            .onMessageReceived(applicationContext, prioritisedBundle, PushConstants.FCM_PUSH_TYPE)
    }

    /**
     * {@inheritDoc}
     */
    override fun onNewToken(applicationContext: Context?, token: String?): Boolean = try {
        PushNotificationHandler
            .getPushNotificationHandler()
            .onNewToken(applicationContext, token, PushConstants.FCM)

        Logger.d(LOG_TAG, "${FCM_LOG_TAG}New token received from FCM - $token")
        true
    } catch (t: Throwable) {
        // do nothing
        Logger.d(LOG_TAG, "${FCM_LOG_TAG}Error onNewToken", t)
        false
    }

    /**
     * {@inheritDoc}
     *
     * Use this method if you are rendering notification by your own and wants to support your custom rendered
     * notification for Pull Notifications
     */
    override fun processPushAmp(context: Context?, message: RemoteMessage) {
        parser.toBundle(message)?.let { CleverTapAPI.processPushNotification(context, it) }
    }
}
