package com.clevertap.android.sdk.product_config

import android.content.Context
import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.utils.FileUtils
import androidx.annotation.RestrictTo

/**
 * Factory class to get [CTProductConfigController] instance for a particular configuration
 *
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Suppress("DEPRECATION")
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object CTProductConfigFactory {

    /**
     * The nullable parameters mirror the (still nullable) SDK call sites; every caller passes real
     * instances, which the controller requires.
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    fun getInstance(
        context: Context?,
        deviceInfo: DeviceInfo,
        config: CleverTapInstanceConfig,
        baseAnalyticsManager: BaseAnalyticsManager?,
        coreMetaData: CoreMetaData?,
        callbackManager: BaseCallbackManager?
    ): CTProductConfigController {
        val requiredContext = requireNotNull(context) { "context is required" }
        val fileUtils = FileUtils(requiredContext, config)
        val configSettings = ProductConfigSettings(deviceInfo.deviceID, config, fileUtils)
        return CTProductConfigController(
            requiredContext,
            config,
            requireNotNull(baseAnalyticsManager) { "analyticsManager is required" },
            requireNotNull(coreMetaData) { "coreMetaData is required" },
            requireNotNull(callbackManager) { "callbackManager is required" },
            configSettings,
            fileUtils
        )
    }
}
