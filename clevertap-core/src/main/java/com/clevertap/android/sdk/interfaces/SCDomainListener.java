package com.clevertap.android.sdk.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Notifies about the availability of SC domain
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SCDomainListener {

    /**
     *  Callback to hand over the SC-domain once it's available
     * @param domain the domain to be used by SC SDK
     */
    void onSCDomainAvailable(@NonNull String domain);

    /**
     *  Callback to notify the unavailability of domain
     */
    void onSCDomainUnavailable();
}