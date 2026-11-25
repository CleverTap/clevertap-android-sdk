package com.clevertap.android.hms

import com.clevertap.android.sdk.pushnotification.PushType

open class HmsConstants {

    companion object {
        const val HMS_DELIVERY_TYPE: String = "hps"
        const val HPS_PROPERTY_REG_ID: String = "hps_token"
        const val CT_HUAWEI_PROVIDER_CLASS: String = "com.clevertap.android.hms.HmsPushProvider"
        const val HUAWEI_SDK_CLASS: String = "com.huawei.hms.push.HmsMessageService"
        const val HMS_LOG_TAG: String = "HPS"

        @JvmField
        val HPS: PushType = PushType(
            HMS_DELIVERY_TYPE,
            HPS_PROPERTY_REG_ID,
            CT_HUAWEI_PROVIDER_CLASS,
            HUAWEI_SDK_CLASS
        )
        const val HCM_SCOPE: String = "HCM"
        const val APP_ID_KEY: String = "client/app_id"
        const val MIN_CT_ANDROID_SDK_VERSION: Int = 30800
    }
}
