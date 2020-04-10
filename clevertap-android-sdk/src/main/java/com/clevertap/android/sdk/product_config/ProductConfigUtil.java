package com.clevertap.android.sdk.product_config;

class ProductConfigUtil {

    //TODO @atul Do we need a separate class for one static method?
    static boolean isSupportedDataType(Object object) {
        return object instanceof String || object instanceof Number || object instanceof Boolean;
    }
}