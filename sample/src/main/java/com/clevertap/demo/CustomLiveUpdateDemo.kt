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
 * Delivered), rendered by the app's [CustomNotificationFactory] (custom RemoteViews). Because the
 * push carries `wzrk_la` and its `data` object has NO `pt_id`, the SDK routes it to the registered
 * factory — the same path a real BE campaign (which nests the custom fields in `data`) would take.
 *
 * Pair this with [ProgressLiveUpdateDemo] (native ProgressStyle) to see both flows side by side.
 */
object CustomLiveUpdateDemo {

    private const val CHANNEL_ID = "live_updates_channel"
    private const val ACTIVITY_ID = "sample_order_custom"
    private const val STEP_GAP_MS = 6000L

    private data class Step(val status: String, val eta: String, val step: Int, val event: String)

    private val steps = listOf(
        Step("Order confirmed", "50 min", 0, "update"),
        Step("Preparing your order", "40 min", 1, "update"),
        Step("Out for delivery", "12 min", 2, "update"),
        Step("Delivered — enjoy!", "0 min", 3, "end")
    )

    fun start(context: Context) {
        val ct = CleverTapAPI.getDefaultInstance(context) ?: return
        ensureChannel(context)
        val appContext = context.applicationContext
        val runId = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        steps.forEachIndexed { i, step ->
            handler.postDelayed({ Thread { render(appContext, ct, step, runId) }.start() }, i * STEP_GAP_MS)
        }
    }

    private fun render(context: Context, ct: CleverTapAPI, step: Step, runId: Long) {
        // wzrk_la + no pt_id + a registered factory => Mode A (factory renders). The factory reads the
        // custom fields directly; the SDK derives the notification id from wzrk_activityId (in place).
        val b = Bundle().apply {
            putString("wzrk_pn", "true")
            putString("wzrk_id", "0_${runId}_${step.step}")
            putString("wzrk_pid", "pid_${runId}_${step.step}")
            putString("wzrk_cid", CHANNEL_ID)
            putString("wzrk_la", "true")
            putString("wzrk_activityId", ACTIVITY_ID)
            putString("wzrk_la_event", step.event)
            putString("la_store", "Pizza place")
            putString("la_items", "2 Pizza")
            putString("la_order", "Order #C-2087")
            putString("la_eta", step.eta)
            putString("la_status", step.status)
            putString("la_step", step.step.toString())
        }
        // Renderer arg is ignored on the Mode A (factory) path; passed to satisfy the API.
        ct.renderPushNotificationOnCallerThread(TemplateRenderer(context, b), context, b)
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
