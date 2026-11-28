package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Interface definition for a callback to be invoked whenever push notification payload is received.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface CTPushNotificationListener {

    /**
     * Receives a callback whenever push notification payload is received.
     */
    void onNotificationClickedPayloadReceived(@NonNull HashMap<String, Object> payload);
}
