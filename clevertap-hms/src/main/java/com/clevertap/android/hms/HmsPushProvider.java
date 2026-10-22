package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_SDK_NAME;
import static com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION;
import static com.clevertap.android.hms.HmsConstants.HPS;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushType;

/**
 * Clevertap's Huawei Plugin Ref: {@link CTPushProvider}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HmsPushProvider implements CTPushProvider {

    private final @NonNull
    CTPushProviderListener ctPushListener;

    private @NonNull
    IHmsSdkHandler hmsSdkHandler;

    @SuppressLint(value = "unused")
    public HmsPushProvider(@NonNull CTPushProviderListener ctPushListener, Context context,
            CleverTapInstanceConfig config) {
        this.ctPushListener = ctPushListener;
        this.hmsSdkHandler = new HmsSdkHandler(context, config);
        try {
            CleverTapAPI.registerCustomSdkVersion(HMS_SDK_NAME, BuildConfig.VERSION_CODE);
        } catch (Throwable t) {
            // Older Core without registerCustomSdkVersion, the version is just not reported
        }
    }

    @NonNull
    @Override
    public PushType getPushType() {
        return HPS;
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