package com.clevertap.android.sdk.product_config;

public interface CTProductConfigListener {
    void onInitSuccess();

    void onInitFailed();

    void onFetched();

    void onActivated();
}