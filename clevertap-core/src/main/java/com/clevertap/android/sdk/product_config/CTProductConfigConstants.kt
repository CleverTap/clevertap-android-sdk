package com.clevertap.android.sdk.product_config;

import java.util.concurrent.TimeUnit;

public interface CTProductConfigConstants {

    String TAG_PRODUCT_CONFIG = "[Product Config]";
    String DIR_PRODUCT_CONFIG = "Product_Config";
    String FILE_NAME_ACTIVATED = "activated.json";
    String FILE_NAME_CONFIG_SETTINGS = "config_settings.json";
    String PRODUCT_CONFIG_JSON_KEY_FOR_KEY = "n";
    String PRODUCT_CONFIG_JSON_KEY_FOR_VALUE = "v";
    String KEY_LAST_FETCHED_TIMESTAMP = "ts";

    int DEFAULT_NO_OF_CALLS = 5;
    int DEFAULT_WINDOW_LENGTH_MINS = 60;

    long DEFAULT_MIN_FETCH_INTERVAL_SECONDS = TimeUnit.MINUTES
            .toSeconds(DEFAULT_WINDOW_LENGTH_MINS / DEFAULT_NO_OF_CALLS);

    //static values
    String DEFAULT_VALUE_FOR_STRING = "";
    Boolean DEFAULT_VALUE_FOR_BOOLEAN = false;
    Long DEFAULT_VALUE_FOR_LONG = 0L;
    Double DEFAULT_VALUE_FOR_DOUBLE = 0.0;

    //arp
    String PRODUCT_CONFIG_NO_OF_CALLS = "rc_n";
    String PRODUCT_CONFIG_WINDOW_LENGTH_MINS = "rc_w";
    String PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS = "fetch_min_interval_seconds";
}