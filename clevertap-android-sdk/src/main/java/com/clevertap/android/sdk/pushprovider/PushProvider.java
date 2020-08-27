package com.clevertap.android.sdk.pushprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

/**
 * Defines a push provider.
 */
public abstract class PushProvider {
    @NonNull
    protected Context context;
    @NonNull
    protected CleverTapInstanceConfig config;

    public PushProvider(@NonNull Context context, @NonNull CleverTapInstanceConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * Returns the platform type. Value must be either {@link PushConstants#ANDROID_PLATFORM}.
     *
     * @return The platform type.
     */
    @PushConstants.Platform
    public abstract int getPlatform();

    /**
     * Returns the delivery type.
     *
     * @return The push delivery type Ref{@link com.clevertap.android.sdk.pushprovider.PushConstants.PushType}.
     */
    @NonNull
    public abstract PushConstants.PushType getPushType();

    /**
     * Gets the push registration token.
     *
     * @return The registration ID.
     */
    @Nullable
    public abstract String getRegistrationToken();

    /**
     * If the underlying push provider is currently available.
     *
     * @return {@code true} if the push provider is currently available, otherwise {@code false}.
     */
    public abstract boolean isAvailable();

    /**
     * If the underlying push provider is supported on the device.
     *
     * @return {@code true} if the push provider is supported on the device, otherwise {@code false}.
     */
    public abstract boolean isSupported();

    /**
     * The minimum SDK version code to support the provider.
     *
     * @return int - the minimum SDK Version Code
     */
    public abstract int minSDKSupportVersionCode();

    @Override
    public String toString() {
        return "PushProvider{" +
                "pushType=" + getPushType() + " className:" + getClass() + "}";
    }

    protected String logTag() {
        return getClass().getSimpleName() + ": ";
    }
}