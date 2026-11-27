package com.clevertap.android.sdk.pushnotification;

import java.util.HashMap;

/**
 * Interface definition for a callback to be invoked whenever push notification payload is received.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
 */
public interface CTPushNotificationListener {

    /**
     * Receives a callback whenever push notification payload is received.
     */
    void onNotificationClickedPayloadReceived(HashMap<String, Object> payload);
}
