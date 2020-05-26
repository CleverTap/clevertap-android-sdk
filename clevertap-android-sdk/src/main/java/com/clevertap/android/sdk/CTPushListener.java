package com.clevertap.android.sdk;

import android.os.Bundle;

/**
 * Interface definition for a callback to be invoked whenever push amp payload is received.
 */
public interface CTPushListener {
    /**
     * Receives a callback whenever push amp payload is received.
     */
    void onPushPayloadReceived(Bundle extras);
}
