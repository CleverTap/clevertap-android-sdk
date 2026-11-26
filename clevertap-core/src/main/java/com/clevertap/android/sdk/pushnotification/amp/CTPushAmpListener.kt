package com.clevertap.android.sdk.pushnotification.amp

import android.os.Bundle

/**
 * Interface definition for a callback to be invoked whenever push amp payload is received.
 */
public fun interface CTPushAmpListener {

    /**
     * Receives a callback whenever push amp payload is received.
     */
    public fun onPushAmpPayloadReceived(extras: Bundle?)
}
