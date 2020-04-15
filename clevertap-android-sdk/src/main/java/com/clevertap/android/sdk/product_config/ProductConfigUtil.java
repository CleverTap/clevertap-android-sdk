package com.clevertap.android.sdk.product_config;

class ProductConfigUtil {

    static boolean isSupportedDataType(Object object) {
        return object instanceof String || object instanceof Number || object instanceof Boolean;
    }
}