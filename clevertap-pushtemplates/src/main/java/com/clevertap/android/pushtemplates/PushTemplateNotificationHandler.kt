package com.clevertap.android.pushtemplates

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Renders Push Template payloads. Register it with
 * `CleverTapAPI.setNotificationHandler(PushTemplateNotificationHandler())` so that core passes pushes
 * with a `pt_id` to it.
 *
 * [onMessageReceived] renders the push with a [TemplateRenderer] synchronously on the calling thread
 * and always returns `true`; [onNewToken] does nothing and returns `true`. [onActionButtonClick]
 * handles action buttons of templates sent with `pt_dismiss_on_click=true`: it dismisses the
 * notification (first raising the payload's custom event for a "remind" action) and returns `true`;
 * otherwise it returns `false`.
 */
public open class PushTemplateNotificationHandler : ActionButtonClickHandler {

    override fun onActionButtonClick(context: Context?, extras: Bundle?, notificationId: Int): Boolean {
        // A null extras threw here in Java as well (the caller logs the failure).
        requireNotNull(extras) { "onActionButtonClick needs the notification extras" }
        val actionID = extras.getString(PTConstants.PT_ACTION_ID)
        val dismissOnClick = extras.getString(PTConstants.PT_DISMISS_ON_CLICK)
        @Suppress("DEPRECATION")
        val config: CleverTapInstanceConfig? = extras.getParcelable("config")

        if (!dismissOnClick.equals("true", ignoreCase = true)) {
            return false
        }
        // For input box remind CTA,pt_dismiss_on_click must be true to raise event
        if (actionID?.contains("remind") == true) {
            Utils.raiseCleverTapEvent(context, config, extras)
        }
        Utils.cancelNotification(context, notificationId)
        return true
    }

    override fun onMessageReceived(applicationContext: Context?, message: Bundle?, pushType: String?): Boolean {
        try {
            PTLog.debug("Inside Push Templates")
            // initial setup
            val cleverTapAPI = CleverTapAPI.getGlobalInstance(
                applicationContext,
                PushNotificationUtil.getAccountIdFromNotificationBundle(message)
            )
            if (cleverTapAPI != null) {
                val config = cleverTapAPI.getInstanceConfig()
                // Java dereferenced both unchecked; the catch below logs the failure.
                val templateRenderer: INotificationRenderer = TemplateRenderer(
                    requireNotNull(applicationContext) { "No application context" },
                    requireNotNull(message) { "No message" },
                    config
                )
                cleverTapAPI.renderPushNotificationOnCallerThread(templateRenderer, applicationContext, message)
            }
        } catch (throwable: Throwable) {
            PTLog.verbose("Error parsing FCM payload", throwable)
        }
        return true
    }

    override fun onNewToken(applicationContext: Context?, token: String?, pushType: PushType?): Boolean = true
}
