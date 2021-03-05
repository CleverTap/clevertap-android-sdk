package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.APP_ID_KEY;
import static com.clevertap.android.hms.HmsConstants.HCM_SCOPE;
import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.content.Context;
import android.text.TextUtils;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;

/**
 * Implementation of {@link IHmsSdkHandler}
 */
class HmsSdkHandler implements IHmsSdkHandler {

    private final Context context;

    private final CleverTapInstanceConfig mConfig;

    HmsSdkHandler(final Context context, final CleverTapInstanceConfig config) {
        this.context = context.getApplicationContext();
        mConfig = config;
    }

    @Override
    public String appId() {
        String appId = null;
        try {
            appId = AGConnectServicesConfig.fromContext(context).getString(APP_ID_KEY);
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, HMS_LOG_TAG + "HMS availability check failed.");
        }
        return appId;
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(appId());
    }

    @Override
    public boolean isSupported() {
        try {
            return HuaweiApiAvailability.getInstance().isHuaweiMobileNoticeAvailable(context) == 0;
        } catch (Throwable e) {
            mConfig.log(LOG_TAG, HMS_LOG_TAG + "HMS is supported check failed.");
            return false;
        }
    }

    @Override
    public String onNewToken() {
        String token = null;
        try {
            String appId = appId();
            if (!TextUtils.isEmpty(appId)) {
                token = HmsInstanceId.getInstance(context).getToken(appId, HCM_SCOPE);
            }
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, HMS_LOG_TAG + "Error requesting HMS token", t);
        }
        return token;
    }
}