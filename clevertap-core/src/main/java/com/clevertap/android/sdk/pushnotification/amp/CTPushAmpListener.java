package com.clevertap.android.sdk.pushnotification.amp;

import android.os.Bundle;

/**
 * Interface definition for a callback to be invoked whenever push amp payload is received.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
 */
public interface CTPushAmpListener {

    /**
     * Receives a callback whenever push amp payload is received.
     */
    void onPushAmpPayloadReceived(Bundle extras);
}
