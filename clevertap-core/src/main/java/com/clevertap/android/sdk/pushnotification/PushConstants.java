package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

public interface PushConstants {

    String LOG_TAG = "PushProvider";

    String FCM_LOG_TAG = PushType.FCM.toString();

    @NonNull
    String FCM_DELIVERY_TYPE = "fcm";
    @NonNull
    String CT_FIREBASE_PROVIDER_CLASS = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider";
    String FIREBASE_SDK_CLASS = "com.google.firebase.messaging.FirebaseMessagingService";
    String FCM_PROPERTY_REG_ID = "fcm_token";
    String FCM_PUSH_TYPE_LOG_NAME = "FCM";
}