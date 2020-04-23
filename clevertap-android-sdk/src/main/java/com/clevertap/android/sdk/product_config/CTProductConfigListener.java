package com.clevertap.android.sdk.product_config;

/**
 * public interface for product config callbacks
 */
public interface CTProductConfigListener {
    void onInitSuccess();

    void onInitFailed();

    void onFetched();

    void onActivated();
}