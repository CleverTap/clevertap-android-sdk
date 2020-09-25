package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.APP_ID_KEY;
import static com.clevertap.android.hms.HmsConstants.HCM_SCOPE;

import android.text.TextUtils;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;

public class HmsSdkHandler implements IHmsSdkHandler {

    private final CTPushProviderListener ctPushListener;

    HmsSdkHandler(CTPushProviderListener ctPushListener) {
        this.ctPushListener = ctPushListener;
    }

    @Override
    public String appId() {
        String appId = null;
        try {
            appId = AGConnectServicesConfig.fromContext(ctPushListener.context()).getString(APP_ID_KEY);
        } catch (Exception e) {
            ctPushListener.config().log(HmsConstants.LOG_TAG, "HMS availability check failed.");
        }
        return appId;
    }

    @Override
    public boolean isSupported() {
        try {
            return HuaweiApiAvailability.getInstance().isHuaweiMobileNoticeAvailable(ctPushListener.context()) == 0;
        } catch (Exception e) {
            ctPushListener.config().log(HmsConstants.LOG_TAG, "HMS is supported check failed.");
            return false;
        }
    }

    @Override
    public String onNewToken() {
        String token = null;
        try {
            String appId = appId();
            if (!TextUtils.isEmpty(appId)) {
                token = HmsInstanceId.getInstance(ctPushListener.context()).getToken(appId, HCM_SCOPE);
            }
        } catch (Throwable t) {
            ctPushListener.config().log(HmsConstants.LOG_TAG, "Error requesting HMS token", t);
        }
        return token;
    }
}