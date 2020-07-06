package com.clevertap.android.sdk;

/**
 * Interface definition for a callback to be invoked when Feature Flag gets updated.
 */
public interface CTFeatureFlagsListener {
    /**
     * Receives a callback whenever feature flags get updated {@link com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController} object
     */
    void featureFlagsUpdated();
}
