package com.clevertap.android.sdk.product_config;

/**
 * Interface definition for a callback to be invoked when Product Config APIs are invoked
 */
public interface CTProductConfigListener {

    /**
     * Receives a callback whenever Product Config gets activated.
     */
    void onActivated();

    /**
     * Receives a callback whenever Product Config is fetched.
     */
    void onFetched();

    /**
     * Receives a callback whenever Product Config initialises.
     */
    void onInit();
}