package com.clevertap.android.sdk.product_config;

/**
 * Interface definition for a callback to be invoked when Product Config APIs are invoked
 */
@Deprecated
public interface CTProductConfigListener {

    /**
     * Receives a callback whenever Product Config gets activated.
     */
    @Deprecated
    void onActivated();

    /**
     * Receives a callback whenever Product Config is fetched.
     */
    @Deprecated
    void onFetched();

    /**
     * Receives a callback whenever Product Config initialises.
     */
    @Deprecated
    void onInit();
}