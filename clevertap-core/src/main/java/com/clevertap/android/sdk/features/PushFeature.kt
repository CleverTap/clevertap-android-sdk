package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.response.PushAmpResponse
import org.json.JSONObject

/**
 * Push notification feature
 * Manages push notification providers (FCM, HMS, etc.)
 */
internal class PushFeature() : CleverTapFeature {

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
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing push amp response"
            )
            return
        }
        pushAmpResponse.processResponse(response, context, coreContract.database(), pushProviders, pushAmpListener)
    }

}
