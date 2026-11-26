package com.clevertap.android.hms

import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Huawei (HMS) push constants, e.g. the `HPS` push type used to identify the Huawei push provider.
 */
public open class HmsConstants {

    public companion object {
        public const val HMS_DELIVERY_TYPE: String = "hps"
        public const val HPS_PROPERTY_REG_ID: String = "hps_token"
        public const val CT_HUAWEI_PROVIDER_CLASS: String = "com.clevertap.android.hms.HmsPushProvider"
        public const val HUAWEI_SDK_CLASS: String = "com.huawei.hms.push.HmsMessageService"
        public const val HMS_LOG_TAG: String = "HPS"

        @JvmField
        public val HPS: PushType = PushType(
            HMS_DELIVERY_TYPE,
            HPS_PROPERTY_REG_ID,
            CT_HUAWEI_PROVIDER_CLASS,
            HUAWEI_SDK_CLASS
        )
        public const val HCM_SCOPE: String = "HCM"
        public const val APP_ID_KEY: String = "client/app_id"
        public const val MIN_CT_ANDROID_SDK_VERSION: Int = 30800
    }
}
