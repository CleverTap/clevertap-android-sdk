package com.clevertap.android.hms;

import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;

interface HmsConstants {

    String HMS_LOG_TAG = PushType.HPS.toString();
    String HCM_SCOPE = "HCM";
    String APP_ID_KEY = "client/app_id";
    int MIN_CT_ANDROID_SDK_VERSION = 30800;

}