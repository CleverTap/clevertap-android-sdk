package com.clevertap.android.sdk.pushnotification

import android.app.Notification
import android.content.Context
import android.os.Bundle

/**
 * Factory interface that gives the client complete control over creating
 * push notifications for CleverTap pushes.
 *
 * When registered, the SDK will invoke [onCreateNotification] for CleverTap Live Activity
 * (live update) pushes instead of using its built-in renderers. The client is responsible for:
 * - Building the [Notification]
 * - Setting its channel id (recommended: the payload's `wzrk_cid`)
 *
 * The SDK still handles:
 * - Push deduplication
 * - Silent push detection
 * - Displaying the notification via [android.app.NotificationManager]
 * - Creating the notification's channel at default importance if it does not already exist,
 *   so the notification is never silently dropped on Android O+
 * - **The notification ID** — owned entirely by the SDK and derived deterministically from the
 *   backend-assigned `wzrk_activityId`, so successive updates for the same activity land on
 *   the same notification (in-place update). The client does not supply an id.
 * - The "Live Activity" lifecycle events (Started / Updated / Ended / Dismissed)
 * - Push notification analytics (viewed events)
 * - TTL and push ID storage
 *
 * Register via [com.clevertap.android.sdk.CleverTapAPI.setNotificationFactory]
 */
interface ICleverTapNotificationFactory {

    /**
     * Called when a CleverTap Live Activity push needs to be rendered.
     *
     * @param context The application context.
     * @param extras  The notification payload bundle containing all CleverTap keys.
     * @return The built [Notification] to display, or `null` to skip rendering this notification.
     *         The SDK owns the notification id (derived from `wzrk_activityId`).
     */
    fun onCreateNotification(context: Context, extras: Bundle): Notification?
}
