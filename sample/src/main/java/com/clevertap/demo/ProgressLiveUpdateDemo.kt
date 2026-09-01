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
 * On Android 16+ the SDK renders it as a native `Notification.ProgressStyle` (status-bar chip,
 * always-expanded, optionally promoted); below 16 it renders the segmented RemoteViews fallback.
 * Both are driven by the same `pt_progress_*` payload.
 *
 * [Variant] lets the sample menu exercise the different feature combinations (actions + deep link,
 * countdown chip + promotion, indeterminate + no-promotion, start/end icons) so each can be
 * eyeballed on device. They all share the same in-place stepping harness.
 */
object ProgressLiveUpdateDemo {

    /** Which feature combination to demo. Each maps to a row under PUSH TEMPLATES. */
    enum class Variant {
        /** Segments + points + text chip. The baseline order tracker. */
        DEFAULT,

        /** Baseline + tap deep link (wzrk_dl) + two action buttons (wzrk_acts). */
        ACTIONS,

        /** Live countdown chip (chip_type=countdown + pt_when) and explicit promotion request. */
        COUNTDOWN,

        /** Same tracker, but promotion turned OFF (pt_promote=false) — no status-bar chip on 16+. */
        NON_PROMOTED,

        /** Adds start + end icons and styled-by-progress coloring. */
        ICONS
    }

    private const val CHANNEL_ID = "live_updates_channel"
    private const val NOTIF_ID = 778899 // fixed id -> successive stages update in place
    private const val STEP_GAP_MS = 6000L

    private const val COLOR_DONE = "#4CAF50"
    private const val COLOR_ACTIVE = "#FF9500"
    private const val COLOR_PENDING = "#48484A"

    private const val TRACKER_ICON = "https://imgur.com/6DavQwg.jpg"
    private const val START_ICON = "https://imgur.com/6DavQwg.jpg"
    private const val END_ICON = "https://imgur.com/6DavQwg.jpg"

    private data class Step(val status: String, val eta: String, val index: Int)

    private val steps = listOf(
        Step("Order confirmed", "50 min", 0),
        Step("Preparing your order", "40 min", 1),
        Step("Out for delivery", "12 min", 2),
        Step("Delivered — enjoy!", "0 min", 3)
    )

    /** Kicks off the timed, in-place progress sequence for [variant]. */
    @JvmOverloads
    fun start(context: Context, variant: Variant = Variant.DEFAULT) {
        val ct = CleverTapAPI.getDefaultInstance(context) ?: return
        ensureChannel(context)
        val appContext = context.applicationContext
        val runId = System.currentTimeMillis() // fresh ids each run so re-tapping always renders
        val handler = Handler(Looper.getMainLooper())
        steps.forEachIndexed { i, step ->
            handler.postDelayed({ Thread { render(appContext, ct, step, runId, variant) }.start() }, i * STEP_GAP_MS)
        }
    }

    private fun render(context: Context, ct: CleverTapAPI, step: Step, runId: Long, variant: Variant) {
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
            putString("pt_progress", progressPercent(step.index).toString())
            putString("pt_progress_tracker_icon", TRACKER_ICON)
            putString("pt_progress_segments", segmentsJson(step.index))
            putString("pt_progress_points", pointsJson(step.index))
            applyVariant(this, variant, step)
        }
        ct.renderPushNotificationOnCallerThread(TemplateRenderer(context, b), context, b)
    }

    /** Layers the variant-specific keys onto the baseline payload. */
    private fun applyVariant(b: Bundle, variant: Variant, step: Step) {
        when (variant) {
            Variant.DEFAULT -> {
                b.putString("pt_chip_type", "text")
                b.putString("pt_chip_text", step.eta)
            }

            Variant.ACTIONS -> {
                b.putString("pt_chip_type", "text")
                b.putString("pt_chip_text", step.eta)
                b.putString("wzrk_dl", "https://clevertap.com") // tapping the notification opens this
                b.putString("wzrk_acts", actionsJson())          // up to 3 buttons, each with its own dl
            }

            Variant.COUNTDOWN -> {
                // A live-updating chip counting down to the ETA, plus an explicit promotion request.
                b.putString("pt_chip_type", "countdown")
                b.putString("pt_when", (System.currentTimeMillis() + etaMillis(step)).toString())
                b.putString("pt_countdown", "true")
                b.putString("pt_promote", "true")
            }

            Variant.NON_PROMOTED -> {
                // Identical tracker with promotion off: on 16+ there's no status-bar chip, so you can
                // compare the promoted vs non-promoted presentation of the same content.
                b.putString("pt_chip_type", "text")
                b.putString("pt_chip_text", step.eta)
                b.putString("pt_promote", "false")
            }

            Variant.ICONS -> {
                b.putString("pt_chip_type", "text")
                b.putString("pt_chip_text", step.eta)
                b.putString("pt_progress_start_icon", START_ICON)
                b.putString("pt_progress_end_icon", END_ICON)
                b.putString("pt_styled_by_progress", "true")
            }
        }
    }

    // 4 steps mapped to a 0..100 progress value for the native bar.
    private fun progressPercent(step: Int): Int = (step * 100) / (steps.size - 1)

    private fun etaMillis(step: Step): Long =
        (step.eta.filter { it.isDigit() }.toLongOrNull() ?: 0L) * 60_000L

    // Two standard action buttons (wzrk_acts): keys id (required), l (label), dl (deep link), ac (auto-cancel).
    private fun actionsJson(): String {
        val arr = JSONArray()
        arr.put(
            JSONObject()
                .put("id", "track").put("l", "Track order")
                .put("dl", "https://clevertap.com/track").put("ac", false)
        )
        arr.put(
            JSONObject()
                .put("id", "support").put("l", "Support")
                .put("dl", "https://clevertap.com/support").put("ac", true)
        )
        return arr.toString()
    }

    // 3 connectors between 4 points: done up to the current step, else pending.
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
