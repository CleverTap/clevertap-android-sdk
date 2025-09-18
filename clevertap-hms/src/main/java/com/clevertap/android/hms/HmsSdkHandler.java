package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.APP_ID_KEY;
import static com.clevertap.android.hms.HmsConstants.HCM_SCOPE;
import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.huawei.agconnect.AGConnectInstance;
import com.huawei.agconnect.AGConnectOptions;
import com.huawei.agconnect.AGConnectOptionsBuilder;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiAvailability;

import org.jetbrains.annotations.TestOnly;

/**
 * Implementation of {@link IHmsSdkHandler}
 */
class HmsSdkHandler implements IHmsSdkHandler {

    private final Context context;
    private final CleverTapInstanceConfig mConfig;
    private volatile AGConnectOptions options;

    HmsSdkHandler(final Context context, final CleverTapInstanceConfig config) {
        this.context = context.getApplicationContext();
        this.mConfig = config;
    }

    @TestOnly
    HmsSdkHandler(final Context context, final CleverTapInstanceConfig config, final AGConnectOptions op) {
        this.context = context.getApplicationContext();
        mConfig = config;
        options = op;
    }

    private synchronized void initializeOptions() {
        if (options != null) {
            return;
        }
        try {
            // Try existing AGConnect instance first
            AGConnectInstance instance = AGConnectInstance.getInstance();
            if (instance != null && instance.getOptions() != null) {
                options = instance.getOptions();
                return;
            }

            // Fallback to building new options
            options = new AGConnectOptionsBuilder().build(context);

        } catch (Exception e) {
            mConfig.log(LOG_TAG, HMS_LOG_TAG + "Failed to initialize AGConnect options: " + e.getMessage());
        }
    }

    @Override
    public String appId() {
        if (options == null) {
            initializeOptions();
        }

        if (options == null) {
            return null;
        }

        try {
            return options.getString(APP_ID_KEY);
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, HMS_LOG_TAG + "Failed to get HMS app ID: " + t.getMessage());
            return null;
        }
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