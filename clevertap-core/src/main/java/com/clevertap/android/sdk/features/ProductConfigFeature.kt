package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.features.callbacks.ProductConfigClientCallbacks
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.response.ProductConfigResponse
import org.json.JSONObject

/**
 * ProductConfigFeature feature
 * Manages remote product configuration
 */
@Deprecated("This is deprecated since clevertap version 5.0.0")
internal class ProductConfigFeature(
    val callbacks: ProductConfigClientCallbacks = ProductConfigClientCallbacks()
) : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var productConfigController: CTProductConfigController? = null

    // Lazy-initialized ProductConfig dependencies (initialized after coreContract is set)
    val productConfigResponse: ProductConfigResponse by lazy {
        ProductConfigResponse(
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
        // Arp handling happens irrespective of analytics mode
        response.optJSONObject("arp")?.let { arp ->
            if (arp.length() > 0) {
                productConfigController?.setArpValue(arp)
            }
        }

        // Intentional as per legacy code, we process arp.
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing Product Config response"
            )
            return
        }
        productConfigResponse.processResponse(response, productConfigController, coreContract.coreMetaData())
    }
}
