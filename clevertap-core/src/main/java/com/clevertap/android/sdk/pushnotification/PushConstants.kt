package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PushConstants {

    public const val LOG_TAG: String = "PushProvider"

    public const val FCM_LOG_TAG: String = "FCM"

    public const val FCM_DELIVERY_TYPE: String = "fcm"

    public const val CT_FIREBASE_PROVIDER_CLASS: String =
        "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider"

    public const val FIREBASE_SDK_CLASS: String = "com.google.firebase.messaging.FirebaseMessagingService"

    public const val FCM_PROPERTY_REG_ID: String = "fcm_token"

    public const val FCM_PUSH_TYPE: String = "FCM"

    @JvmField
    public val FCM: PushType = PushType(
        FCM_DELIVERY_TYPE,
        FCM_PROPERTY_REG_ID,
        CT_FIREBASE_PROVIDER_CLASS,
        FIREBASE_SDK_CLASS
    )
}
