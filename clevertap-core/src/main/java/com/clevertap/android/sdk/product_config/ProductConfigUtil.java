package com.clevertap.android.sdk.product_config;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.TAG_PRODUCT_CONFIG;

class ProductConfigUtil {

    static boolean isSupportedDataType(Object object) {
        return object instanceof String || object instanceof Number || object instanceof Boolean;
    }

    static String getLogTag(CleverTapInstanceConfig config) {
        return (config != null ? config.getAccountId() : "") + TAG_PRODUCT_CONFIG;
    }
}