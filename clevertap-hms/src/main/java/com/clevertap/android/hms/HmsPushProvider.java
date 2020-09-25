package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HmsPushProvider implements CTPushProvider {

    private CTPushProviderListener ctPushListener;

    private IHmsSdkHandler hmsSdkHandler;

    @SuppressLint(value = "unused")
    public HmsPushProvider(@NonNull CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
        this.hmsSdkHandler = new HmsSdkHandler(ctPushListener);
    }

    @Override
    public int getPlatform() {
        return ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return PushConstants.PushType.HPS;
    }

    @Override
    public boolean isAvailable() {
        boolean isAvailable = false;
        if (hmsSdkHandler != null) {
            isAvailable = !TextUtils.isEmpty(hmsSdkHandler.appId());
        } else {
            ctPushListener.config().log(LOG_TAG, "isNotAvailable since hmsSdkHandler is null");
        }
        return isAvailable;
    }

    @Override
    public boolean isSupported() {
        boolean isSupported = false;
        if (hmsSdkHandler != null) {
            isSupported = hmsSdkHandler.isSupported();
        } else {
            ctPushListener.config().log(LOG_TAG, "Not Supported since hmsSdkHandler is null");
        }
        return isSupported;
    }

    @Override
    public int minSDKSupportVersionCode() {
        return HmsConstants.MIN_CT_ANDROID_SDK_VERSION;
    }

    @Override
    public void requestToken() {
        String token = null;
        if (hmsSdkHandler != null) {
            token = hmsSdkHandler.onNewToken();
        } else {
            ctPushListener.config().log(LOG_TAG, "requestToken failed since hmsSdkHandler is null");
        }
        if (ctPushListener != null) {
            ctPushListener.onNewToken(token, getPushType());
        }
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setHmsSdkHandler(IHmsSdkHandler hmsSdkHandler) {
        this.hmsSdkHandler = hmsSdkHandler;
    }

}