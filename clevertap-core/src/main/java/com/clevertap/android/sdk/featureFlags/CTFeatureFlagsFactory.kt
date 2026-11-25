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

    @JvmStatic
    fun getInstance(
        context: Context?, guid: String?, config: CleverTapInstanceConfig,
        callbackManager: BaseCallbackManager?, analyticsManager: BaseAnalyticsManager?
    ): CTFeatureFlagsController {
        val fileUtils = FileUtils(context, config)
        return CTFeatureFlagsController(guid, config, callbackManager, analyticsManager, fileUtils)
    }
}
