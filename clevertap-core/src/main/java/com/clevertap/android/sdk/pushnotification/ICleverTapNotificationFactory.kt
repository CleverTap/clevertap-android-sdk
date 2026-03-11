package com.clevertap.android.sdk.pushnotification

import android.app.Notification
import android.content.Context
import android.os.Bundle

/**
 * Factory interface that gives the client complete control over creating
 * push notifications for CleverTap pushes.
 *
 * When registered, the SDK will invoke [onCreateNotification] instead
 * of using its built-in renderers. The client is responsible for:
 * - Creating the notification channel
 * - Building the [Notification]
 * - Choosing the notification ID
 *
 * The SDK still handles:
 * - Push deduplication
 * - Silent push detection
 * - Displaying the notification via [android.app.NotificationManager]
 * - Push notification analytics (viewed events)
 * - TTL and push ID storage
 *
 * Register via [com.clevertap.android.sdk.CleverTapAPI.setNotificationFactory]
 */
interface ICleverTapNotificationFactory {

    /**
     * Called when a CleverTap push notification needs to be rendered.
     *
     * This method is called on a background thread. You may perform
     * blocking operations such as downloading images if needed.
     *
     * @param context The application context.
     * @param extras  The notification payload bundle containing all CleverTap keys.
     * @return A [NotificationResult] containing the built notification and notification ID,
     *         or `null` to skip rendering this notification.
     */
    fun onCreateNotification(context: Context, extras: Bundle): NotificationResult?

    /**
     * Holds the result of a custom notification build.
     */
    class NotificationResult(
        val notification: Notification,
        val notificationId: Int
    )
}
