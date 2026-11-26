package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo

/**
 * Defines a push provider.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CTPushProvider {

    /**
     * Returns the delivery type.
     *
     * @return The push delivery type Ref[PushType].
     */
    public val pushType: PushType

    /**
     * If the underlying push provider is currently available.
     *
     * @return `true` if the push provider is currently available, otherwise `false`.
     */
    public val isAvailable: Boolean

    /**
     * If the underlying push provider is supported on the device.
     *
     * @return `true` if the push provider is supported on the device, otherwise `false`.
     */
    public val isSupported: Boolean

    /**
     * The minimum SDK version code to support the provider.
     *
     * @return int - the minimum SDK Version Code
     */
    public fun minSDKSupportVersionCode(): Int

    /**
     * Requests the push registration token.
     */
    public fun requestToken()
}
