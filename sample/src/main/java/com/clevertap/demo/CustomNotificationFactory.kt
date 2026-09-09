package com.clevertap.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.pushnotification.ICleverTapNotificationFactory

/**
 * Sample [ICleverTapNotificationFactory] — **Mode A (client renders)** — showing how a client can
 * build a **progress-centric** Live Update themselves, without the SDK's `pt_progress` template.
 *
 * On **Android 16+** it uses `NotificationCompat.ProgressStyle` directly (the sample app ships
 * `androidx.core:core:1.17.0`), so it's the promoted / always-expanded native live update.
 * **Below 16** it falls back to the classic determinate progress-bar notification, which works on
 * every version.
 *
 * ### Payload the factory reads (client-defined keys)
 * The BE nests these inside the `data` object; the SDK surfaces them to the top-level bundle, so the
 * factory reads them as flat extras (see the JSON shape shared with BE):
 * - `nt`               title
 * - `la_status`        status line (e.g. "Out for delivery")
 * - `la_eta`           ETA text (shown as the status-bar chip on 16+)
 * - `la_progress`      current progress, 0..`la_progress_max`
 * - `la_progress_max`  max (default 100)
 *
 * The SDK owns the wrapper: routing (`wzrk_la`), the in-place id (`wzrk_activityId`), the lifecycle
 * events (Started/Updated/Ended/Dismissed) and the impression ("Notification Viewed"). This factory
 * builds the Notification and, because a client-rendered notification owns its own content intent,
 * wires the click so CleverTap records "Notification Clicked" (see [contentIntent]).
 */
class CustomNotificationFactory : ICleverTapNotificationFactory {

    companion object {
        private const val CHANNEL_ID = "live_updates_channel"
        private const val KEY_CHANNEL = "wzrk_cid"

        // Client-defined render keys (BE nests them inside `data`; SDK surfaces them flat).
        private const val KEY_TITLE = "nt"
        private const val KEY_STATUS = "la_status"
        private const val KEY_ETA = "la_eta"
        private const val KEY_PROGRESS = "la_progress"
        private const val KEY_PROGRESS_MAX = "la_progress_max"

        private const val ACCENT = 0xFFFF9500.toInt()
        private const val COLOR_DONE = 0xFF4CAF50.toInt()
        private const val COLOR_PENDING = 0xFF48484A.toInt()

        // Android 16 (Baklava) introduced NotificationCompat.ProgressStyle + promotion.
        private const val API_PROGRESS_STYLE = 36
    }

    override fun onCreateNotification(context: Context, extras: Bundle): Notification? {
        val channelId = extras.getString(KEY_CHANNEL)?.takeIf { it.isNotBlank() } ?: CHANNEL_ID
        createChannel(context, channelId)

        val ended = isEnded(extras)
        val title = extras.getString(KEY_TITLE) ?: "Your order"
        val status = extras.getString(KEY_STATUS).orEmpty()
        val eta = extras.getString(KEY_ETA)
        val max = extras.getString(KEY_PROGRESS_MAX)?.toIntOrNull()?.coerceAtLeast(1) ?: 100
        val progress = (extras.getString(KEY_PROGRESS)?.toIntOrNull() ?: 0).coerceIn(0, max)

        val nb = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(if (eta.isNullOrEmpty()) status else "$status • ETA $eta")
            .setOnlyAlertOnce(true)          // updates should not re-alert
            .setOngoing(!ended)              // ongoing while the order is live
            .setAutoCancel(ended)

        // A fully client-rendered notification owns its own content intent, so the SDK does NOT wire
        // click tracking for it (unlike core/template pushes). Open the app with the push extras;
        // because MyApplication registers ActivityLifecycleCallback, CleverTap raises "Notification
        // Clicked" automatically when the launched screen resumes. If your app does not register
        // ActivityLifecycleCallback, call
        // CleverTapAPI.getDefaultInstance(context)?.pushNotificationClickedEvent(extras) yourself
        // instead — but never both, or the click is counted twice. ("Notification Viewed" is always
        // raised by the SDK, so the client should not raise it here either.)
        nb.setContentIntent(contentIntent(context, extras))

        if (Build.VERSION.SDK_INT >= API_PROGRESS_STYLE) {
            // Android 16+ : native, promotable ProgressStyle (client uses androidx.core 1.17.0 directly).
            val segments = ArrayList<NotificationCompat.ProgressStyle.Segment>()
            if (progress > 0) {
                segments.add(NotificationCompat.ProgressStyle.Segment(progress).setColor(COLOR_DONE))
            }
            if (max - progress > 0) {
                segments.add(NotificationCompat.ProgressStyle.Segment(max - progress).setColor(COLOR_PENDING))
            }
            val style = NotificationCompat.ProgressStyle().setProgress(progress)
            if (segments.isNotEmpty()) style.setProgressSegments(segments)
            nb.setStyle(style)
            eta?.takeIf { it.isNotEmpty() }?.let { nb.setShortCriticalText(it) }
            if (!ended) nb.setRequestPromotedOngoing(true)
        } else {
            // Below 16 : the classic determinate progress-bar notification (all versions).
            nb.setProgress(max, progress, false)
            if (status.isNotEmpty()) nb.setSubText(status)
        }

        return nb.build()
    }

    /**
     * Opens the app on tap, carrying the push extras so CleverTap can attribute the click to this
     * push (the extras hold `wzrk_pn`/`wzrk_id`/`wzrk_pid`/`wzrk_acct_id`).
     */
    private fun contentIntent(context: Context, extras: Bundle): PendingIntent {
        val launch = Intent(context, HomeScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtras(extras)
        }
        // Stable request code per activity so successive in-place updates refresh the same
        // PendingIntent (FLAG_UPDATE_CURRENT) rather than leaking one per update.
        val requestCode = extras.getString("wzrk_activityId")?.hashCode() ?: 0
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, requestCode, launch, flags)
    }

    private fun isEnded(extras: Bundle): Boolean =
        "end".equals(extras.getString("wzrk_la_event"), ignoreCase = true)

    private fun createChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Live Updates", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Order tracking and other live updates"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
