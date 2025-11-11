package com.clevertap.android.sdk.features

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CTFeatureFlagsListener
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsFactory
import com.clevertap.android.sdk.response.FeatureFlagResponse
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Feature Flag feature
 * Manages feature flag evaluation and callbacks
 */
internal class FeatureFlagFeature : CleverTapFeature {

    private var listener: WeakReference<CTFeatureFlagsListener> = WeakReference(null)

    var ctFeatureFlagsController: CTFeatureFlagsController? = null
        set(value) {
            value?.setFeatureFlagListener(listener)
        }

    lateinit var coreContract: CoreContract

    // Lazy-initialized FeatureFlag dependencies (initialized after coreContract is set)
    val featureFlagResponse: FeatureFlagResponse by lazy {
        FeatureFlagResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
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

    /**
     * Phase 1: Reset method moved from CoreState
     * Resets feature flags for the current user with new device ID
     */
    fun reset(deviceId: String?) {
        val controller = ctFeatureFlagsController
        if (controller != null && controller.isInitialized()) {
            controller.resetWithGuid(deviceId)
            controller.fetchFeatureFlags()
        } else {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "Can't reset Feature Flags, CTFeatureFlagsController is null or not initialized"
            )
        }
    }

    /**
     * Phase 2: Initialization method moved from CoreState
     * Initializes Feature Flags with the provided device ID
     */
    @WorkerThread
    fun initialize(deviceId: String?) {
        coreContract.logger().verbose(
            coreContract.config().accountId + ":async_deviceID",
            "Initializing Feature Flags with device Id = $deviceId"
        )
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().debug(
                coreContract.config().accountId,
                "Feature Flag is not enabled for this instance"
            )
        } else {
            ctFeatureFlagsController = CTFeatureFlagsFactory.getInstance(
                coreContract.context(),
                deviceId,
                coreContract.config(),
                coreContract.analytics()
            )
            coreContract.logger().verbose(
                coreContract.config().accountId + ":async_deviceID",
                "Feature Flags initialized"
            )
        }
    }
}
