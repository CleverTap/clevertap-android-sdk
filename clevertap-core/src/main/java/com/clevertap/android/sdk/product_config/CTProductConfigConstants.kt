package com.clevertap.android.sdk.product_config

import java.util.concurrent.TimeUnit

internal object CTProductConfigConstants {

    const val TAG_PRODUCT_CONFIG = "[Product Config]"
    const val DIR_PRODUCT_CONFIG = "Product_Config"
    const val FILE_NAME_ACTIVATED = "activated.json"
    const val FILE_NAME_CONFIG_SETTINGS = "config_settings.json"
    const val PRODUCT_CONFIG_JSON_KEY_FOR_KEY = "n"
    const val PRODUCT_CONFIG_JSON_KEY_FOR_VALUE = "v"
    const val KEY_LAST_FETCHED_TIMESTAMP = "ts"

    const val DEFAULT_NO_OF_CALLS = 5
    const val DEFAULT_WINDOW_LENGTH_MINS = 60

    val DEFAULT_MIN_FETCH_INTERVAL_SECONDS: Long =
        TimeUnit.MINUTES.toSeconds((DEFAULT_WINDOW_LENGTH_MINS / DEFAULT_NO_OF_CALLS).toLong())

    //static values
    const val DEFAULT_VALUE_FOR_STRING = ""

    const val DEFAULT_VALUE_FOR_BOOLEAN = false

    const val DEFAULT_VALUE_FOR_LONG = 0L

    const val DEFAULT_VALUE_FOR_DOUBLE = 0.0

    //arp
    const val PRODUCT_CONFIG_NO_OF_CALLS = "rc_n"
    const val PRODUCT_CONFIG_WINDOW_LENGTH_MINS = "rc_w"
    const val PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS = "fetch_min_interval_seconds"
}
