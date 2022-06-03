package com.clevertap.android.sdk.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Notifies about the availability of DC domain
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DCDomainCallback {

    /**
     *  Callback to hand over the DC-domain once it's available
     * @param domain the domain to be used by DC SDK
     */
    void onDCDomainAvailable(@NonNull String domain);

    /**
     *  Callback to notify the unavailability of domain
     */
    void onDCDomainUnavailable();

}