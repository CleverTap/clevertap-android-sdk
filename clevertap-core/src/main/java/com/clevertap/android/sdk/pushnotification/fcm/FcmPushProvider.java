package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

/**
 * Clevertap's Firebase Plugin Ref: {@link CTPushProvider}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint(value = "unused")
public class FcmPushProvider implements CTPushProvider {

    private IFcmSdkHandler handler;

    @SuppressLint(value = "unused")
    public FcmPushProvider(CTPushProviderListener ctPushListener, Context context, CleverTapInstanceConfig config) {
        handler = new FcmSdkHandlerImpl(ctPushListener, context, config);
    }

    @Override
    public int getPlatform() {
        return ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return handler.getPushType();
    }

    /**
     * App supports FCM
     *
     * @return boolean true if FCM services are available
     */
    @Override
    public boolean isAvailable() {
        return handler.isAvailable();
    }

    /**
     * Device supports FCM
     *
     * @return - true if FCM is supported in the platform
     */
    @Override
    public boolean isSupported() {
        return handler.isSupported();
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 0;// supporting FCM from base version
    }

    @Override
    public void requestToken() {
        handler.requestToken();
    }

    void setHandler(final IFcmSdkHandler handler) {
        this.handler = handler;
    }
}