package com.clevertap.android.sdk.featureFlags

import android.content.Context
import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.utils.FileUtils

/**
 * Factory class to get [CTFeatureFlagsController] instance for a particular configuration
 */
@Suppress("DEPRECATION")
internal object CTFeatureFlagsFactory {

    /**
     * The nullable parameters mirror the (still nullable) SDK call sites; every caller passes real
     * instances, which the controller requires.
     */
    fun getInstance(
        context: Context?,
        guid: String?,
        config: CleverTapInstanceConfig,
        callbackManager: BaseCallbackManager?,
        analyticsManager: BaseAnalyticsManager?
    ): CTFeatureFlagsController = CTFeatureFlagsController(
        guid,
        config,
        requireNotNull(callbackManager) { "callbackManager is required" },
        requireNotNull(analyticsManager) { "analyticsManager is required" },
        FileUtils(requireNotNull(context) { "context is required" }, config)
    )
}
