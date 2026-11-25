package com.clevertap.android.pushtemplates

import android.content.Context
import android.content.Intent
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver
import com.clevertap.android.sdk.task.CTExecutorFactory

internal class PTPushNotificationReceiver : CTPushNotificationReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //Removes the deeplink key from bundle if deepLink is not present and launch activity.
        if (intent.getStringExtra(Constants.DEEP_LINK_KEY) == null) {
            intent.removeExtra(Constants.DEEP_LINK_KEY)
        }

        super.onReceive(context, intent)

        val extras = intent.extras ?: return
        val cleverTapAPI = CleverTapAPI
            .getGlobalInstance(context, extras.getString(Constants.WZRK_ACCT_ID_KEY))

        if (cleverTapAPI != null) {
            try {
                val config = cleverTapAPI.getInstanceConfig()
                val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
                task.execute("PTPushNotificationReceiver#cleanUpFiles") {
                    try {
                        Utils.deleteImageFromStorage(context, intent)
                        Utils.deleteSilentNotificationChannel(context)
                    } catch (t: Throwable) {
                        PTLog.verbose(
                            "Couldn't clean up images and/or couldn't delete silent notification channel: "
                                    + t.localizedMessage
                        )
                    }
                    null
                }
            } catch (e: Exception) {
                PTLog.verbose(
                    "Couldn't clean up images and/or couldn't delete silent notification channel: " + e
                        .localizedMessage
                )
            }
        } else {
            PTLog.verbose("clevertap instance is null, not running PTPushNotificationReceiver#cleanUpFiles")
        }
    }
}
