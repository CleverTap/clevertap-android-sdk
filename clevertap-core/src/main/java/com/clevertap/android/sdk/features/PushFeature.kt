package com.clevertap.android.sdk.features

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.response.PushAmpResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Push notification feature
 * Manages push notification providers (FCM, HMS, etc.)
 */
internal class PushFeature(
    val notifyNotificationRendered: (String) -> Unit = { listenerKey ->
        val nrl = CleverTapAPI.getNotificationRenderedListener(listenerKey)
        nrl?.onNotificationRendered(true)
    }
) : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var pushAmpListener: CTPushAmpListener? = null
    var pushNotificationListener: CTPushNotificationListener? = null

    // Lazy-initialized Push dependencies (initialized after coreContract is set)
    private val ctWorkManager: CTWorkManager by lazy {
        CTWorkManager(coreContract.context(), coreContract.config())
    }

    val pushProviders: PushProviders by lazy {
        PushProviders(
            coreContract.context(),
            coreContract.config(),
            coreContract.database(),
            coreContract.validationResultStack(),
            coreContract.analytics(),
            ctWorkManager,
            coreContract.executors(),
            coreContract.clock()
        )
    }

    val pushAmpResponse: PushAmpResponse by lazy {
        PushAmpResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    override fun handleApiData(
        response: JSONObject
    ) {
        // Handle Pull Notifications response
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing push amp response"
            )
            return
        }
        pushAmpResponse.processResponse(response, coreContract.context(), coreContract.database(), pushProviders, pushAmpListener)
    }

    /**
     * Handles the click event of a push notification.
     *
     * Raises a "Notification Clicked" event to be sent
     * to the CleverTap backend and notifies the [CTPushNotificationListener] if one is registered.
     *
     * @param extras The bundle data received from the push notification payload.
     */
    fun handlePushNotificationClicked(extras: Bundle) {
        // Regular push notification click - track via analytics
        val sent: Boolean = coreContract.analytics().pushNotificationClickedEvent(extras)
        if (sent) {
            pushNotificationListener?.onNotificationClickedPayloadReceived(
                Utils.convertBundleObjectToHashMap(extras)
            )
        }
    }

    /**
     * Phase 3: Push impression tracking moved from CoreState
     * Notifies listeners when push impressions are sent to server
     */
    fun notifyPushImpressionsSentToServer(queue: JSONArray) {
        for (i in 0..<queue.length()) {
            try {
                val notif = queue.getJSONObject(i).optJSONObject("evtData")
                if (notif != null) {
                    val pushId = notif.optString(Constants.WZRK_PUSH_ID)
                    val pushAccountId = notif.optString(Constants.WZRK_ACCT_ID_KEY)

                    val key = PushNotificationUtil
                        .buildPushNotificationRenderedListenerKey(pushAccountId, pushId)
                    notifyNotificationRendered(key)
                }
            } catch (_: JSONException) {
                coreContract.logger().verbose(
                    coreContract.config().accountId,
                    "Encountered an exception while parsing the push notification viewed event queue"
                )
            }
        }

        coreContract.logger().verbose(
            coreContract.config().accountId,
            "push notification viewed event sent successfully"
        )
    }
}
