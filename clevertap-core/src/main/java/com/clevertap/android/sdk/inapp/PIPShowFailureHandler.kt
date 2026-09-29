package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType

/**
 * Handles PIP show failure — reports the media failure as a `wzrk_error`, clears the
 * in-app display lock, and advances the queue.
 *
 * Separated from [InAppListener] so that
 * [com.clevertap.android.sdk.InAppNotificationActivity] (which only hosts standard
 * in-app fragments) is not forced to implement PIP-specific callbacks.
 */
internal fun interface PIPShowFailureHandler {
    fun onPIPShowFailed(inAppNotification: CTInAppNotification, mediaType: PIPMediaType)
}
