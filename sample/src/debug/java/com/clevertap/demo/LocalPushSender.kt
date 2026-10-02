package com.clevertap.demo

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import org.json.JSONObject

/**
 * Renders a push payload locally, without a campaign, by handing it to the same entry point the FCM
 * listener uses. Shared by [PushTestReceiver] (adb) and [LocalPushActivity] (on-device JSON editor).
 */
object LocalPushSender {

    private const val DEFAULT_CHANNEL_ID = "PTTesting"

    /**
     * Turns a JSON object into the flat string-to-string bundle an FCM data payload actually is.
     *
     * Nested objects and arrays are serialised back to their JSON text, which is how the dashboard
     * sends `pt_json` too, so a payload copied from a campaign behaves the same here.
     */
    fun bundleFromJson(json: String): Bundle {
        val obj = JSONObject(json)
        val bundle = Bundle()
        obj.keys().forEach { key ->
            val value = obj.get(key)
            bundle.putString(key, if (value is String) value else value.toString())
        }
        return bundle
    }

    /**
     * Fills in the keys the SDK needs to recognise the bundle as a CleverTap push, then renders it
     * off the main thread - the templates renderer downloads images inline.
     *
     * `wzrk_id` is deliberately left alone: without it the SDK skips the duplicate check and raises
     * no Notification Viewed event, so local test renders stay out of the dashboard's analytics.
     */
    fun render(context: Context, extras: Bundle) {
        val appContext = context.applicationContext
        if (!extras.containsKey("wzrk_pn")) extras.putString("wzrk_pn", "true")
        if (!extras.containsKey("wzrk_cid")) extras.putString("wzrk_cid", DEFAULT_CHANNEL_ID)
        if (!extras.containsKey("wzrk_acct_id")) {
            accountId(appContext)?.let { extras.putString("wzrk_acct_id", it) }
        }

        Thread {
            PushNotificationHandler.getPushNotificationHandler()
                .onMessageReceived(appContext, extras, PushConstants.FCM_PUSH_TYPE)
        }.start()
    }

    /** Reads the account the app is configured with, so the payload never carries a stale hardcoded id. */
    private fun accountId(context: Context): String? = runCatching {
        context.packageManager
            .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData?.getString("CLEVERTAP_ACCOUNT_ID")
    }.getOrNull()
}
