package com.clevertap.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.app.Notification
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.pushnotification.ICleverTapNotificationFactory

/**
 * Sample [ICleverTapNotificationFactory] demonstrating **CleverTap Live Updates** with an
 * order-tracking notification (see SDK-6039). It resembles an iOS Live Activity order tracker:
 * a food-order card that updates in place as the order progresses through
 * Placed -> Preparing -> En Route -> Delivered.
 *
 * ### How the demo works
 * The backend/FCM sends a Live Update data push carrying:
 * - `wzrk_la = "true"`            marks the push as a Live Update (SDK routes it here)
 * - `cleverTapActivityId`        stable order id — the SDK derives the notification id from it,
 *                                so every update lands on the **same** notification (in place)
 * - `wzrk_la_event`              `update` (default) or `end` (terminal / Delivered)
 * - demo order fields (below)    store, items, order id, ETA, status, step index
 *
 * This factory only builds the [android.app.Notification]. The SDK owns the notification id,
 * dismissal tracking, and the "Live Activity" lifecycle events (Started/Updated/Ended/Dismissed).
 *
 * Non Live-Update pushes fall back to a simple text notification.
 */
class CustomNotificationFactory : ICleverTapNotificationFactory {

    companion object {
        // Fallback channel id, used only when the push carries no wzrk_cid.
        private const val CHANNEL_ID = "live_updates_channel"
        private const val KEY_CHANNEL = "wzrk_cid" // channel id supplied by the CleverTap payload

        // Demo payload keys (client-defined; a real integration would align these with the BE).
        private const val KEY_STORE = "la_store"
        private const val KEY_ITEMS = "la_items"
        private const val KEY_ORDER_ID = "la_order"
        private const val KEY_ETA = "la_eta"
        private const val KEY_STATUS = "la_status"
        private const val KEY_STEP = "la_step" // 0=Placed, 1=Preparing, 2=En Route, 3=Delivered

        private val STEP_DOTS = intArrayOf(R.id.dot_0, R.id.dot_1, R.id.dot_2, R.id.dot_3)
        private val STEP_LINES = intArrayOf(R.id.line_0, R.id.line_1, R.id.line_2)
        private val STEP_LABELS = intArrayOf(R.id.tv_step_0, R.id.tv_step_1, R.id.tv_step_2, R.id.tv_step_3)

        private const val COLOR_ACTIVE = 0xFFFF9500.toInt()
        private const val COLOR_INACTIVE = 0xFF48484A.toInt()
        private const val COLOR_LABEL_ON = 0xFFFFFFFF.toInt()
        private const val COLOR_LABEL_OFF = 0xFF8E8E93.toInt()
    }

    override fun onCreateNotification(
        context: Context,
        extras: Bundle
    ): Notification? {

        // Option 3: use the channel id supplied in the payload (wzrk_cid); fall back to our own.
        // We still create it here so the sample is robust standalone; the SDK will also create
        // it at default importance if it is ever missing (safety net).
        val channelId = extras.getString(KEY_CHANNEL)?.takeIf { it.isNotBlank() } ?: CHANNEL_ID
        createChannel(context, channelId)

        // Android has no "always expanded" live-activity mode: the collapsed view is height
        // limited, so we give it a compact single-line + progress-bar layout, and put the full
        // 4-step tracker in the expanded (big) view.
        val collapsedView = buildCollapsedView(context, extras)
        val expandedView = buildOrderTrackerView(context, extras)

        // The SDK owns the notification id (derived from cleverTapActivityId), so we just build
        // and return the Notification.
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(COLOR_ACTIVE)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(collapsedView)
            .setOngoing(!isEnded(extras)) // ongoing while the order is live; dismissible once ended
            .setOnlyAlertOnce(true) // updates should not re-alert on every push
            .setAutoCancel(isEnded(extras))
            .build()
    }

    private fun buildOrderTrackerView(context: Context, extras: Bundle): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notification_live_order)

        rv.setTextViewText(R.id.tv_store, extras.getString(KEY_STORE) ?: "Pizza place")
        rv.setTextViewText(R.id.tv_items, extras.getString(KEY_ITEMS) ?: "2 Pizza")
        rv.setTextViewText(R.id.tv_order_id, extras.getString(KEY_ORDER_ID) ?: "Order #123456")
        rv.setTextViewText(R.id.tv_eta_value, extras.getString(KEY_ETA) ?: "--")
        rv.setTextViewText(R.id.tv_status, extras.getString(KEY_STATUS) ?: "Order confirmed")

        // An `end` push forces the final Delivered step regardless of the supplied index.
        val step = if (isEnded(extras)) STEP_DOTS.lastIndex else currentStep(extras)
        applyProgress(rv, step)
        return rv
    }

    private fun buildCollapsedView(context: Context, extras: Bundle): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notification_live_order_collapsed)
        rv.setTextViewText(R.id.tv_store, extras.getString(KEY_STORE) ?: "Pizza place")
        rv.setTextViewText(R.id.tv_status, extras.getString(KEY_STATUS) ?: "Order confirmed")
        rv.setTextViewText(R.id.tv_eta_value, extras.getString(KEY_ETA) ?: "--")
        val step = if (isEnded(extras)) STEP_DOTS.lastIndex else currentStep(extras)
        rv.setProgressBar(R.id.progress_bar, STEP_DOTS.lastIndex, step, false)
        return rv
    }

    /** Colours the dots/connectors/labels up to (and including) [step]. */
    private fun applyProgress(rv: RemoteViews, step: Int) {
        for (i in STEP_DOTS.indices) {
            val reached = i <= step
            rv.setInt(
                STEP_DOTS[i], "setBackgroundResource",
                if (reached) R.drawable.live_order_dot_active else R.drawable.live_order_dot_inactive
            )
            rv.setTextColor(STEP_LABELS[i], if (reached) COLOR_LABEL_ON else COLOR_LABEL_OFF)
        }
        for (i in STEP_LINES.indices) {
            rv.setInt(
                STEP_LINES[i], "setBackgroundColor",
                if (i < step) COLOR_ACTIVE else COLOR_INACTIVE
            )
        }
    }

    private fun currentStep(extras: Bundle): Int {
        val raw = extras.getString(KEY_STEP)?.toIntOrNull() ?: 0
        return raw.coerceIn(0, STEP_DOTS.lastIndex)
    }

    private fun isEnded(extras: Bundle): Boolean =
        "end".equals(extras.getString("wzrk_la_event"), ignoreCase = true)

    private fun createChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Live Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Order tracking and other live updates"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
