package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CTFeatureFlagsListener
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController
import com.clevertap.android.sdk.response.FeatureFlagResponse
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Feature Flag feature
 * Manages feature flag evaluation and callbacks
 */
internal data class FeatureFlagFeature(
    val featureFlagResponse: FeatureFlagResponse
) : CleverTapFeature {

    private var listener: WeakReference<CTFeatureFlagsListener> = WeakReference(null)

    var ctFeatureFlagsController: CTFeatureFlagsController? = null
        set(value) {
            value?.setFeatureFlagListener(listener)
        }

    lateinit var coreContract: CoreContract

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
    ) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing Feature Flags response"
            )
            return
        }
        featureFlagResponse.processResponse(response, ctFeatureFlagsController)
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    fun attachListener(listener: CTFeatureFlagsListener) {
        this.listener = WeakReference(listener)
        ctFeatureFlagsController?.setFeatureFlagListener(this.listener)
    }
}
