package com.clevertap.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.CleverTapAPI
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sample showcase for the **progress-centric Live Update** (`pt_progress`).
 *
 * Fires an order-tracking Live Update and advances it in place over a few seconds
 * (Placed → Preparing → En Route → Delivered) — rendered locally through the CleverTap
 * Push Template renderer, so it works out of the box without a dashboard campaign.
 *
 * On Android 16+ the SDK renders it as a native, promoted `Notification.ProgressStyle`
 * (status-bar chip, always-expanded); below 16 it renders the segmented RemoteViews fallback.
 * Both are driven by the same `pt_progress_*` payload.
 */
object ProgressLiveUpdateDemo {

    private const val CHANNEL_ID = "live_updates_channel"
    private const val ACTIVITY_ID = "sample_order_live"
    private const val NOTIF_ID = 778899 // fixed id -> successive stages update in place
    private const val STEP_GAP_MS = 6000L

    private const val COLOR_DONE = "#4CAF50"
    private const val COLOR_ACTIVE = "#FF9500"
    private const val COLOR_PENDING = "#48484A"

    private data class Step(val status: String, val eta: String, val index: Int)

    private val steps = listOf(
        Step("Order confirmed", "50 min", 0),
        Step("Preparing your order", "40 min", 1),
        Step("Out for delivery", "12 min", 2),
        Step("Delivered — enjoy!", "0 min", 3)
    )

    /** Kicks off the timed, in-place progress sequence. */
    fun start(context: Context) {
        val ct = CleverTapAPI.getDefaultInstance(context) ?: return
        ensureChannel(context)
        val appContext = context.applicationContext
        val runId = System.currentTimeMillis() // fresh ids each run so re-tapping always renders
        val handler = Handler(Looper.getMainLooper())
        steps.forEachIndexed { i, step ->
            handler.postDelayed({ Thread { render(appContext, ct, step, runId) }.start() }, i * STEP_GAP_MS)
        }
    }

    private fun render(context: Context, ct: CleverTapAPI, step: Step, runId: Long) {
        // Rendered as a pt_progress Push Template. A fixed notificationId makes successive
        // stages replace the same notification (in place). We do NOT set wzrk_la here so this
        // local demo always exercises the pt_progress renderer even if a notification factory
        // is registered (a real BE campaign uses wzrk_la + la_pt_data to drive the same template).
        val b = Bundle().apply {
            putString("wzrk_pn", "true")
            putString("wzrk_id", "0_${runId}_${step.index}")
            putString("wzrk_pid", "pid_${runId}_${step.index}")
            putString("wzrk_cid", CHANNEL_ID)
            putInt("notificationId", NOTIF_ID)
            putString("pt_id", "pt_progress")
            putString("nt", "Order #A1234")
            putString("nm", step.status)
            putString("pt_progress", step.index.toString())
            putString("pt_chip_type", "text")
            putString("pt_chip_text", step.eta)
            putString("pt_progress_tracker_icon", "https://imgur.com/6DavQwg.jpg")
            putString("pt_progress_segments", segmentsJson(step.index))
            putString("pt_progress_points", pointsJson(step.index))
        }
        ct.renderPushNotificationOnCallerThread(TemplateRenderer(context, b), context, b)
    }

    // 3 connectors between 4 points: done up to the current step, active at it, else pending.
    private fun segmentsJson(step: Int): String {
        val arr = JSONArray()
        for (i in 0 until 3) {
            val color = if (i < step) COLOR_DONE else COLOR_PENDING
            arr.put(JSONObject().put("length", 1).put("color", color))
        }
        return arr.toString()
    }

    private fun pointsJson(step: Int): String {
        val arr = JSONArray()
        for (i in 0 until 4) {
            val color = when {
                i < step -> COLOR_DONE
                i == step -> COLOR_ACTIVE
                else -> COLOR_PENDING
            }
            arr.put(JSONObject().put("position", i).put("color", color))
        }
        return arr.toString()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Live Updates", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}
