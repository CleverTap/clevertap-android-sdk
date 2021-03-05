package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.XPS;
import static com.clevertap.android.xps.XpsConstants.MIN_CT_ANDROID_SDK_VERSION;
import static java.lang.Boolean.TRUE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

/**
 * Clevertap's Xiaomi Plugin Ref: {@link CTPushProvider}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XiaomiPushProvider implements CTPushProvider {

    private @NonNull
    final CTPushProviderListener ctPushListener;

    private @NonNull
    IMiSdkHandler miSdkHandler;

    @SuppressLint(value = "unused")
    public XiaomiPushProvider(@NonNull CTPushProviderListener ctPushListener, Context context,
            CleverTapInstanceConfig config) {
        this.ctPushListener = ctPushListener;
        this.miSdkHandler = new XiaomiSdkHandler(context, config);
    }

    @Override
    public int getPlatform() {
        return ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return XPS;
    }

    @Override
    public boolean isAvailable() {
        return miSdkHandler.isAvailable();
    }

    @Override
    public boolean isSupported() {
        return TRUE;
    }

    @Override
    public int minSDKSupportVersionCode() {
        return MIN_CT_ANDROID_SDK_VERSION;
    }

    @Override
    public void requestToken() {
        ctPushListener.onNewToken(miSdkHandler.onNewToken(), getPushType());
    }

    void setMiSdkHandler(@NonNull IMiSdkHandler sdkHandler) {
        this.miSdkHandler = sdkHandler;
    }

}