package com.clevertap.android.sdk.pushnotification

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler

/**
 * Android 12 prevents components that start other activities inside services or broadcast receivers when notification is opened.
 * If this requirement is not met, the system will prevent the activity from starting.
 * As a part of these Android OS changes, CleverTap will be deprecating the CTNotificationIntentService class from v4.3.0
 * CTNotificationIntentService was used to handle Push notifications with action buttons. The service used to open the deeplink
 * and close the notification tray.
 * From Android 12, closing of the notification tray is now handled by the OS when it opens an Activity.
 * If the action buttons are redirecting the user to an existing activity inside the app, then
 * the Notification Clicked event will be triggered with the event properties mentioning which action button was clicked.
 * If the deep link is to a third-party application or a URL, then the Notification Clicked event will not be tracked.
 */
@Deprecated("Deprecated since 4.3.0: Android 12 notification trampoline restrictions.")
@Suppress("DEPRECATION")
open class CTNotificationIntentService : IntentService("CTNotificationIntentService") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        // A null intent threw a NullPointerException in Java; `!!` keeps that.
        val extras = intent!!.extras ?: return

        val notificationHandler = CleverTapAPI.getNotificationHandler()
        val actionButtonClickHandler =
            if (PushNotificationHandler.isForPushTemplates(extras) && notificationHandler != null) {
                notificationHandler as ActionButtonClickHandler
            } else {
                PushNotificationHandler.getPushNotificationHandler() as ActionButtonClickHandler
            }

        val type = extras.getString(Constants.KEY_CT_TYPE) //actionButtonClickHandler.getType(extras);
        if (TYPE_BUTTON_CLICK == type) {
            Logger.v("CTNotificationIntentService handling $TYPE_BUTTON_CLICK")
            handleActionButtonClick(extras, actionButtonClickHandler)
        } else {
            Logger.v("CTNotificationIntentService: unhandled intent ${intent.action}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleActionButtonClick(extras: Bundle, actionButtonClickHandler: ActionButtonClickHandler) {
        try {
            val autoCancel = extras.getBoolean("autoCancel", false)
            val notificationId = extras.getInt("notificationId", -1)
            val dl = extras.getString("dl")

            val context = applicationContext

            val isActionButtonClickHandled =
                actionButtonClickHandler.onActionButtonClick(context, extras, notificationId)
            if (isActionButtonClickHandled) {
                return
            }

            // For Android 12 Trampoline restrictions, do not process deeplink from here
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                return
            }

            val launchIntent = if (dl != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(dl)).also {
                    Utils.setPackageNameFromResolveInfoList(context, it)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName)
            }

            if (launchIntent == null) {
                Logger.v("CTNotificationService: create launch intent.")
                return
            }

            launchIntent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            CleverTapAPI.handleNotificationClicked(context, extras)

            launchIntent.putExtras(extras)
            launchIntent.removeExtra("dl")

            val ptDismissOnClick = extras.getString("pt_dismiss_on_click", "")

            if (autoCancel && notificationId > -1 && ptDismissOnClick.isEmpty()) {
                val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                notificationManager?.cancel(notificationId)
            }
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) // close the notification drawer
            startActivity(launchIntent)
        } catch (t: Throwable) {
            Logger.v("CTNotificationService: unable to process action button click:  ${t.localizedMessage}")
        }
    }

    companion object {

        const val MAIN_ACTION: String = "com.clevertap.PUSH_EVENT"

        const val TYPE_BUTTON_CLICK: String = "com.clevertap.ACTION_BUTTON_CLICK"
    }
}
