package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.response.PushAmpResponse
import org.json.JSONObject

/**
 * Push notification feature
 * Manages push notification providers (FCM, HMS, etc.)
 */
internal data class PushFeature(
    val pushProviders: PushProviders,
    val pushAmpResponse: PushAmpResponse,
    var pushAmpListener: CTPushAmpListener? = null,
    var pushNotificationListener : CTPushNotificationListener? = null
) : CleverTapFeature {

    lateinit var coreContract: CoreContract

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
    ) {
        // Handle Pull Notifications response
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().getAccountId(),
                "CleverTap instance is configured to analytics only, not processing push amp response"
            )
            return
        }
        pushAmpResponse.processResponse(response, stringBody, context)
    }

}
