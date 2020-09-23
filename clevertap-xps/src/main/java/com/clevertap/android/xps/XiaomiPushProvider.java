package com.clevertap.android.xps;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

import static com.clevertap.android.xps.XpsConstants.LOG_TAG;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XiaomiPushProvider implements CTPushProvider {
    private CTPushProviderListener ctPushListener;
    private IMiSdkHandler miSdkHandler;

    @Override
    public void setCTPushListener(CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        setMiSdkHandler(new XiaomiSdkHandler(ctPushListener));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setMiSdkHandler(IMiSdkHandler provider) {
        this.miSdkHandler = provider;
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
        String token = null;
        if (miSdkHandler != null) {
            token = miSdkHandler.onNewToken();
        } else {
            ctPushListener.log(LOG_TAG, "requestToken failed since miSdkHandler is null");
        }
        if (ctPushListener != null) {
            ctPushListener.onNewToken(token, getPushType());
        }
    }

    @Override
    public boolean isAvailable() {
        boolean isAvailable = false;
        if (miSdkHandler != null) {
            isAvailable = !TextUtils.isEmpty(miSdkHandler.appId()) && !TextUtils.isEmpty(miSdkHandler.appKey());
        } else {
            ctPushListener.log(LOG_TAG, "Xiaomi Pushprovider is not available as miSdkHandler is null");
        }
        return isAvailable;
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