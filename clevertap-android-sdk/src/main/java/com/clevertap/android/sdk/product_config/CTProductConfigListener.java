package com.clevertap.android.sdk.product_config;

/**
 * public interface for product config callbacks
 */
public interface CTProductConfigListener {
    void onInit();

    void onFetched();

    void onActivated();
}