package com.clevertap.android.xps;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XiaomiPushProvider implements CTPushProvider {
    private @NonNull
    final CTPushProviderListener ctPushListener;
    private @NonNull
    IMiSdkHandler miSdkHandler;

    @SuppressLint(value = "unused")
    public XiaomiPushProvider(@NonNull CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        this.miSdkHandler = new XiaomiSdkHandler(ctPushListener);
    }

    @VisibleForTesting
    @RestrictTo(value = RestrictTo.Scope.LIBRARY)
    public void setMiSdkHandler(@NonNull IMiSdkHandler sdkHandler) {
        this.miSdkHandler = sdkHandler;
    }

    @Override
    public int getPlatform() {
        return PushConstants.ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return PushConstants.PushType.XPS;
    }

    @Override
    public void requestToken() {
        String token = miSdkHandler.onNewToken();
        ctPushListener.onNewToken(token, getPushType());
    }

    @Override
    public boolean isAvailable() {
        return miSdkHandler.isAvailable();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public int minSDKSupportVersionCode() {
        return XpsConstants.MIN_CT_ANDROID_SDK_VERSION;
    }

}