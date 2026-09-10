package com.clevertap.android.sdk.pushnotification

import android.app.Notification
import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI

/**
 * Gives the client full control over building the [Notification] for a CleverTap Live Activity
 * (live update) push. Register via [CleverTapAPI.setNotificationFactory]; the SDK then calls
 * [onCreateNotification] instead of its built-in renderers.
 *
 * The SDK still owns display, channel creation, the notification id (derived from `wzrk_activityId`
 * for in-place updates), and lifecycle/viewed analytics.
 */
interface ICleverTapNotificationFactory {

    /**
     * Called when a CleverTap Live Activity push needs to be rendered.
     *
     * @param context application context
     * @param extras  the push payload bundle with all CleverTap keys
     * @return the [Notification] to display, or `null` to skip rendering
     */
    fun onCreateNotification(context: Context, extras: Bundle): Notification?
}
