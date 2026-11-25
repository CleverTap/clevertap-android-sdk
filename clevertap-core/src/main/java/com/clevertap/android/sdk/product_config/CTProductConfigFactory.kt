package com.clevertap.android.sdk.product_config

import android.content.Context
import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.utils.FileUtils

/**
 * Factory class to get [CTProductConfigController] instance for a particular configuration
 *
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Suppress("DEPRECATION")
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
internal object CTProductConfigFactory {

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    @JvmStatic
    fun getInstance(
        context: Context?, deviceInfo: DeviceInfo,
        config: CleverTapInstanceConfig, baseAnalyticsManager: BaseAnalyticsManager?, coreMetaData: CoreMetaData?,
        callbackManager: BaseCallbackManager?
    ): CTProductConfigController {
        val guid = deviceInfo.deviceID
        val fileUtils = FileUtils(context, config)
        val configSettings = ProductConfigSettings(guid, config, fileUtils)
        return CTProductConfigController(
            context, config, baseAnalyticsManager, coreMetaData, callbackManager,
            configSettings, fileUtils
        )
    }
}
