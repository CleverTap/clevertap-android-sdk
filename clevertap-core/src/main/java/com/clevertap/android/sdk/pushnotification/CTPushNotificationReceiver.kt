package com.clevertap.android.sdk.pushnotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils

/**
 * Android 12 prevents components that start other activities inside services or broadcast receivers when notification is opened.
 * If this requirement is not met, the system will prevent the activity from starting.
 *
 * As a part of these Android OS changes, CleverTap will be deprecating the CTPushNotificationReceiver class from v4.3.0
 *
 * CTPushNotificationReceiver was used to handle Push notifications with deep links pointing inside the app or third-party applications/URLs.
 * The receiver would first raise the Notification Clicked event and then open the Activity as mentioned in the deep link.
 *
 * Android 12 restricts usage of notification trampolines, meaning that notification must start the activity directly on the notification tap.
 * Tracking of Notification Clicked events now happens in the [CleverTapAPI] instance registered by [com.clevertap.android.sdk.ActivityLifecycleCallback].
 * The push payload is attached as an extra to the notification intent and processed when the notification is tapped.
 * If the deep link is to a third-party application or a URL, then the Notification Clicked event will NO LONGER be tracked.
 */
@Deprecated("Deprecated since 4.3.0: Android 12 notification trampoline restrictions.")
open class CTPushNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        try {
            val launchIntent: Intent

            val extras = intent.extras ?: return

            if (extras.containsKey(Constants.DEEP_LINK_KEY)) {
                launchIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(intent.getStringExtra(Constants.DEEP_LINK_KEY))
                )
                Utils.setPackageNameFromResolveInfoList(context, launchIntent)
            } else {
                launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            }

            CleverTapAPI.handleNotificationClicked(context, extras)

            launchIntent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            launchIntent.putExtras(extras)

            //to prevent calling of pushNotificationClickedEvent(extras) in ActivityLifecycleCallback
            launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)

            if ( /*VERSION.SDK_INT < VERSION_CODES.S && */extras.containsKey(Constants.CLOSE_SYSTEM_DIALOGS)) {
                val closeNotificationDrawer = extras.getBoolean(Constants.CLOSE_SYSTEM_DIALOGS)
                if (closeNotificationDrawer) {
                    context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                }

            }

            context.startActivity(launchIntent)

            Logger.d("CTPushNotificationReceiver: handled notification: $extras")
        } catch (t: Throwable) {
            Logger.v("CTPushNotificationReceiver: error handling notification", t)
        }
    }
}
