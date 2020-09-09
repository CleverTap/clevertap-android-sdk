package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Defines a push provider.
 */
public interface CTPushProvider {

    /**
     * Set this listener to communicate with core SDK.
     *
     * @param listener
     */
    void setCTPushListener(CTPushProviderListener listener);

    /**
     * Returns the platform type. Value must be either {@link PushConstants#ANDROID_PLATFORM}.
     *
     * @return The platform type.
     */
    @PushConstants.Platform
    int getPlatform();

    /**
     * Returns the delivery type.
     *
     * @return The push delivery type Ref{@link com.clevertap.android.sdk.pushnotification.PushConstants.PushType}.
     */
    @NonNull
    PushConstants.PushType getPushType();

    /**
     * Gets the push registration token.
     * @param callback - pass the callback to get the token
     */
    @Nullable
    void getRegistrationToken(CTPushRegistrationListener callback);

    /**
     * If the underlying push provider is currently available.
     *
     * @return {@code true} if the push provider is currently available, otherwise {@code false}.
     */
    boolean isAvailable();

    /**
     * If the underlying push provider is supported on the device.
     *
     * @return {@code true} if the push provider is supported on the device, otherwise {@code false}.
     */
    boolean isSupported();

    /**
     * The minimum SDK version code to support the provider.
     *
     * @return int - the minimum SDK Version Code
     */
    int minSDKSupportVersionCode();

}