package com.clevertap.android.hms;

import com.clevertap.android.sdk.pushnotification.PushType;

public class HmsProvider {

    public static PushType HPS = new PushType(
            HmsConstants.HMS_DELIVERY_TYPE,
            HmsConstants.HPS_PROPERTY_REG_ID,
            HmsConstants.CT_HUAWEI_PROVIDER_CLASS,
            HmsConstants.HUAWEI_SDK_CLASS,
            HmsConstants.HMS_LOG_TAG
    );
}
