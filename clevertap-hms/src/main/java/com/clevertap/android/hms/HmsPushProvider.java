package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION;
import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

/**
 * Clevertap's Huawei Plugin Ref: {@link CTPushProvider}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HmsPushProvider implements CTPushProvider {

    private final @NonNull
    CTPushProviderListener ctPushListener;

    private @NonNull
    IHmsSdkHandler hmsSdkHandler;

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return HPS;
    }

    @SuppressLint(value = "unused")
    public HmsPushProvider(@NonNull CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        this.hmsSdkHandler = new HmsSdkHandler(ctPushListener);
    }

    @Override
    public int getPlatform() {
        return ANDROID_PLATFORM;
    }

    @Override
    public boolean isAvailable() {
        return hmsSdkHandler.isAvailable();
    }

    @Override
    public boolean isSupported() {
        return hmsSdkHandler.isSupported();
    }

    @Override
    public int minSDKSupportVersionCode() {
        return MIN_CT_ANDROID_SDK_VERSION;
    }

    @Override
    public void requestToken() {
        ctPushListener.onNewToken(hmsSdkHandler.onNewToken(), getPushType());
    }

    void setHmsSdkHandler(@NonNull final IHmsSdkHandler hmsSdkHandler) {
        this.hmsSdkHandler = hmsSdkHandler;
    }
}