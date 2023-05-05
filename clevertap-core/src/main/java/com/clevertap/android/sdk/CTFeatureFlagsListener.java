package com.clevertap.android.sdk;

/**
 * Interface definition for a callback to be invoked when Feature Flag gets updated.
 * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
 *      Note: This interface has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 * </p>
 */
@Deprecated
public interface CTFeatureFlagsListener {

    /**
     * Receives a callback whenever feature flags get updated {@link com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController}
     * object
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    void featureFlagsUpdated();
}
