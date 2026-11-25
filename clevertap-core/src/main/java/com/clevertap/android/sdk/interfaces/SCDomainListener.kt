package com.clevertap.android.sdk.interfaces

import androidx.annotation.RestrictTo

/**
 * Notifies about the availability of SC domain
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SCDomainListener {

    /**
     * Callback to hand over the SC-domain once it's available
     * @param domain the domain to be used by SC SDK
     */
    fun onSCDomainAvailable(domain: String)

    /**
     * Callback to notify the unavailability of domain
     */
    fun onSCDomainUnavailable()
}
