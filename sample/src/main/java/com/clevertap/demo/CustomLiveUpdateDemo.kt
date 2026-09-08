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

/**
 * Sample showcase for the **custom-factory Live Update (Mode A)**.
 *
 * Fires an order-tracking Live Update that advances in place (Placed → Preparing → En Route →
 * Delivered), rendered by the app's [CustomNotificationFactory] — which builds a **progress-centric**
 * notification itself (native ProgressStyle on 16+, classic progress bar below). Because the push
 * carries `wzrk_la` and its `data` object has NO `pt_id`, the SDK routes it to the registered factory
 * — the same path a real BE campaign (which nests the render fields in `data`) would take.
 *
 * Pair this with [ProgressLiveUpdateDemo] (SDK-rendered pt_progress) to see both flows side by side.
 */
object CustomLiveUpdateDemo {

    private const val CHANNEL_ID = "live_updates_channel"
    private const val ACTIVITY_ID = "sample_order_custom"
    private const val STEP_GAP_MS = 6000L

    // progress is 0..100; the first step "starts" the activity (wzrk_la_event=start).
    private data class Step(val status: String, val eta: String, val progress: Int, val event: String)

    private val steps = listOf(
        Step("Order confirmed", "50 min", 5, "start"),
        Step("Preparing your order", "40 min", 35, "update"),
        Step("Out for delivery", "12 min", 75, "update"),
        Step("Delivered — enjoy!", "0 min", 100, "end")
    )

    fun start(context: Context) {
        val ct = CleverTapAPI.getDefaultInstance(context) ?: return
        ensureChannel(context)
        val appContext = context.applicationContext
        val runId = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        steps.forEachIndexed { i, step ->
            // renderPushNotification posts to the SDK's own worker executor, so no manual Thread needed.
            handler.postDelayed({ render(appContext, ct, step, runId) }, i * STEP_GAP_MS)
        }
    }

    private fun render(context: Context, ct: CleverTapAPI, step: Step, runId: Long) {
        // wzrk_la + no pt_id + a registered factory => Mode A (factory renders). The factory reads the
        // client-defined render fields; the SDK derives the notification id from wzrk_activityId (in
        // place). A real BE push nests the render fields inside a `data` object (which the SDK surfaces
        // to the top level); this local demo sets them flat directly since it bypasses onMessageReceived.
        val b = Bundle().apply {
            putString("wzrk_pn", "true")
            putString("wzrk_id", "0_${runId}_${step.progress}")
            putString("wzrk_pid", "pid_${runId}_${step.progress}")
            putString("wzrk_cid", CHANNEL_ID)
            putString("wzrk_la", "true")
            putString("wzrk_activityId", ACTIVITY_ID)
            putString("wzrk_la_event", step.event)
            // client-defined render keys read by CustomNotificationFactory
            putString("nt", "Pizza Palace — Order #C-2087")
            putString("la_status", step.status)
            putString("la_eta", step.eta)
            putString("la_progress", step.progress.toString())
            putString("la_progress_max", "100")
        }
        // Renderer arg is ignored on the Mode A (factory) path; passed to satisfy the API.
        ct.renderPushNotification(TemplateRenderer(context, b), context, b)
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
