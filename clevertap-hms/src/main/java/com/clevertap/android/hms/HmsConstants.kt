package com.clevertap.android.hms;

import com.clevertap.android.sdk.pushnotification.PushType;

public class HmsConstants {

    public static final String HMS_DELIVERY_TYPE = "hps";
    public static final String HPS_PROPERTY_REG_ID = "hps_token";
    public static final String CT_HUAWEI_PROVIDER_CLASS = "com.clevertap.android.hms.HmsPushProvider";
    public static final String HUAWEI_SDK_CLASS = "com.huawei.hms.push.HmsMessageService";
    public static final String HMS_LOG_TAG = "HPS";
    public static final PushType HPS = new PushType(
            HMS_DELIVERY_TYPE,
            HPS_PROPERTY_REG_ID,
            CT_HUAWEI_PROVIDER_CLASS,
            HUAWEI_SDK_CLASS
    );
    public static final String HCM_SCOPE = "HCM";
    public static final String APP_ID_KEY = "client/app_id";
    public static final int MIN_CT_ANDROID_SDK_VERSION = 30800;
}