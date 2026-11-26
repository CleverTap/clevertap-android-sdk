package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.TAG_PRODUCT_CONFIG

/**
 * Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 */
@Deprecated("This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
internal object ProductConfigUtil {

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    fun getLogTag(config: CleverTapInstanceConfig?): String = "${config?.accountId ?: ""}$TAG_PRODUCT_CONFIG"

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    fun isSupportedDataType(value: Any?): Boolean = value is String || value is Number || value is Boolean
}
