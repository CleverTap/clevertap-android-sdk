package com.clevertap.android.hms;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;

import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HmsPushProvider implements CTPushProvider {
    private static final String LOG_TAG = HmsPushProvider.class.getSimpleName();
    private static final String HCM_SCOPE = "HCM";
    private static final String APP_ID_KEY = "client/app_id";
    private CTPushProviderListener ctPushListener;

    @Override
    public void setCTPushListener(CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
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
    public void requestToken() {
        String token = null;
        try {
            String appId = getAppId();
            if (!TextUtils.isEmpty(appId)) {
                token = HmsInstanceId.getInstance(ctPushListener.context()).getToken(appId, HCM_SCOPE);
            }
        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "Error requesting HMS token", t);
        }
        if (ctPushListener != null) {
            ctPushListener.onNewToken(token, getPushType());
        }
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(getAppId());
    }

    private String getAppId() {
        String appId = null;
        try {
            appId = AGConnectServicesConfig.fromContext(ctPushListener.context()).getString(APP_ID_KEY);
        } catch (Exception e) {
            ctPushListener.log(LOG_TAG, "HMS availability check failed.");
        }
        return appId;
    }

    @Override
    public boolean isSupported() {
        try {
            return HuaweiApiAvailability.getInstance().isHuaweiMobileNoticeAvailable(ctPushListener.context()) == 0;
        } catch (Exception e) {
            ctPushListener.log(LOG_TAG, "HMS is supported check failed.");
            return false;
        }
    }

    @Override
    public int minSDKSupportVersionCode() {
        //TODO -discuss about the use case of this.
        return 30800;
    }

}