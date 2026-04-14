package com.clevertap.android.sdk.inapp

/**
 * Handles PIP show failure — clears the in-app display lock and advances the queue.
 *
 * Separated from [InAppListener] so that
 * [com.clevertap.android.sdk.InAppNotificationActivity] (which only hosts standard
 * in-app fragments) is not forced to implement PIP-specific callbacks.
 */
internal fun interface PIPShowFailureHandler {
    fun onPIPShowFailed(inAppNotification: CTInAppNotification)
}
