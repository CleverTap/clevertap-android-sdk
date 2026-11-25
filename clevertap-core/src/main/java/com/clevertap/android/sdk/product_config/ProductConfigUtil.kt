package com.clevertap.android.sdk.product_config;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.TAG_PRODUCT_CONFIG;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

/**
 * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
 *      Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 * </p>
 */
@Deprecated
class ProductConfigUtil {

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    static String getLogTag(CleverTapInstanceConfig config) {
        return (config != null ? config.getAccountId() : "") + TAG_PRODUCT_CONFIG;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    static boolean isSupportedDataType(Object object) {
        return object instanceof String || object instanceof Number || object instanceof Boolean;
    }
}