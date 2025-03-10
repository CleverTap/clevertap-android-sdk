package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

public interface PushConstants {

    String LOG_TAG = "PushProvider";

    String FCM_LOG_TAG = "FCM";

    @NonNull
    String FCM_DELIVERY_TYPE = "fcm";
    @NonNull
    String CT_FIREBASE_PROVIDER_CLASS = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider";
    String FIREBASE_SDK_CLASS = "com.google.firebase.messaging.FirebaseMessagingService";
    String FCM_PROPERTY_REG_ID = "fcm_token";

    PushType FCM = new PushType(
            FCM_DELIVERY_TYPE,
            FCM_PROPERTY_REG_ID,
            CT_FIREBASE_PROVIDER_CLASS,
            FIREBASE_SDK_CLASS
    );
}