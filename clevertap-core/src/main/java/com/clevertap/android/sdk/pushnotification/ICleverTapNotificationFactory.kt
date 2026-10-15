package com.clevertap.android.sdk.pushnotification

import android.app.Notification
import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI

/**
 * Gives the client full control over building the [Notification] for a CleverTap Live Activity
 * (live update) push. Register via [CleverTapAPI.setNotificationFactory] in `Application.onCreate`
 * (a push can start the app without any Activity); the SDK then calls
 * [onCreateNotification] instead of its built-in renderers (pushes with a `pt_id` are still
 * rendered by Push Templates when delivered through `CTFcmMessageHandler`).
 *
 * The SDK still owns display, channel creation, the notification id (derived from `wzrk_activityId`
 * for in-place updates), and lifecycle/viewed analytics.
 */
fun interface ICleverTapNotificationFactory {

    /**
     * Called when a CleverTap Live Activity push needs to be rendered.
     *
     * @param context the Android context; may be the caller's (e.g. a Service), so don't store it
     * @param extras  the push payload bundle with all CleverTap keys
     * @return the [Notification] to display, or `null` to skip rendering
     */
    fun onCreateNotification(context: Context, extras: Bundle): Notification?
}
