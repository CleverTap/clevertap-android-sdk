package com.clevertap.android.sdk.pushnotification

import java.util.HashMap

/**
 * Interface definition for a callback to be invoked whenever push notification payload is received.
 */
public fun interface CTPushNotificationListener {

    /**
     * Receives a callback whenever push notification payload is received.
     */
    public fun onNotificationClickedPayloadReceived(payload: HashMap<String, Any>?)
}
