package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.PROGRESS_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants
import org.json.JSONArray

/**
 * Progress-centric template (`pt_progress`).
 *
 * Two rendering tiers, gated on the OS:
 * - **Android 16+ (API 36):** native `NotificationCompat.ProgressStyle` — segments, points,
 *   tracker icon, status chip, and promotion (`setRequestPromotedOngoing`). Invoked via reflection
 *   (see [buildNative]) so the SDK needs no build-time dependency on androidx.core 1.17.0.
 *   This is a base style with NO custom RemoteViews, which is what makes promotion possible.
 * - **Below API 36:** a custom-RemoteViews fallback that mimics the same look — tracker icon,
 *   title/message, and a dots-and-connectors progress row (points as dots, segments as weighted
 *   colored connectors). Not promotable (promotion is a 16+ OS feature), but kept ongoing so it
 *   behaves like a live update.
 *
 * Both tiers read the same `pt_progress_*` contract (TAN §14).
 */
internal class ProgressStyle(private val renderer: TemplateRenderer) {

    private data class SegmentData(val length: Int, val color: Int?)
    private data class PointData(val position: Int, val color: Int?)

    companion object {
        // Android 16 (Baklava) introduced Notification.ProgressStyle + promotion.
        private const val API_PROGRESS_STYLE = 36
        // Fallback-only defaults: used by the pre-16 RemoteViews path when a segment/point in the
        // payload omits its own color. On native 16+ colors come from the payload and the platform
        // supplies its own default, so these are never applied there.
        private val COLOR_INACTIVE = Color.parseColor("#48484A")   // inactive segment/track gray
        private val COLOR_POINT_DEFAULT = Color.parseColor("#FFFFFF") // uncolored milestone dot
    }

    fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {

        val ended = "end".equals(extras.getString(PTConstants.PT_LA_EVENT), ignoreCase = true)
        val title = renderer.getTitle(extras, context) ?: ""
        val message = renderer.getMessage(extras) ?: ""
        val segments = parseSegments(extras.getString(PTConstants.PT_PROGRESS_SEGMENTS))
        val points = parsePoints(extras.getString(PTConstants.PT_PROGRESS_POINTS))
        val trackerIcon = bitmap(context, extras.getString(PTConstants.PT_PROGRESS_TRACKER_ICON))

        nb.setSmallIcon(renderer.smallIcon)
            .setContentTitle(Html.fromHtml(title))
            .setContentText(message)
            .setOnlyAlertOnce(true)          // updates should not re-alert
            .setOngoing(!ended)              // ongoing while the activity is live
            .setAutoCancel(ended)
            .setColor(parseColor(renderer.smallIconColour))

        // Prefer the native ProgressStyle on Android 16+, but guard it: androidx.core is a
        // consumer-supplied (compileOnly) dependency, so a host app on core < 1.17.0 at runtime
        // won't have NotificationCompat.ProgressStyle. Rather than force that version on every
        // consumer, we catch the class/method absence and degrade to the RemoteViews fallback.
        val nativeOk = Build.VERSION.SDK_INT >= API_PROGRESS_STYLE &&
            runCatching { buildNative(context, extras, nb, segments, points, trackerIcon, ended) }
                .onFailure { PTLog.verbose("pt_progress: native ProgressStyle unavailable (androidx.core < 1.17.0?), using fallback", it) }
                .isSuccess
        if (!nativeOk) {
            buildFallback(context, extras, nb, title, message, segments, points, trackerIcon)
        }

        // Tap action (deep link / launch) + action buttons — shared across both tiers, reusing
        // the standard Push Template machinery (wzrk_dl content intent, wzrk_acts buttons).
        applyContentIntent(context, extras, notificationId, nb)
        applyActionButtons(context, extras, notificationId, nb)
        return nb
    }

    /** Sets the notification tap intent from the standard deep-link key (wzrk_dl), like BasicStyle. */
    private fun applyContentIntent(context: Context, extras: Bundle, notificationId: Int, nb: NotificationCompat.Builder) {
        val deepLink = extras.getString(Constants.DEEP_LINK_KEY)
        PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true, PROGRESS_CONTENT_PENDING_INTENT, deepLink
        )?.let { nb.setContentIntent(it) }
    }

    /** Adds action buttons from the standard wzrk_acts payload, reusing the shared ActionButtonsHandler. */
    private fun applyActionButtons(context: Context, extras: Bundle, notificationId: Int, nb: NotificationCompat.Builder) {
        val actions = extras.getString(Constants.WZRK_ACTIONS)
            ?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return
        if (actions.length() == 0) return
        renderer.actionButtons = renderer.getActionButtons(context, extras, notificationId, actions)
        ActionButtonsHandler(renderer).addActionButtons(context, extras, notificationId, nb)
    }

    // --- Android 16+ : native ProgressStyle + promotion (via reflection) ---
    //
    // NotificationCompat.ProgressStyle + the promotion/chip builder methods only exist in
    // androidx.core 1.17.0. We invoke them REFLECTIVELY so this SDK compiles WITHOUT a build-time
    // dependency on 1.17.0 (no compileOnly / resolutionStrategy force needed): the host app supplies
    // whatever androidx.core it ships. If that runtime core is < 1.17.0 the reflected class/methods
    // are absent -> the first Class.forName throws -> builderFromStyle's runCatching falls back to
    // the RemoteViews path. The literal class/method names below track androidx.core 1.17.0.
    // Everything already present in the baseline core (IconCompat, setStyle/Style, setWhen/chronometer)
    // is called directly and type-safely.

    private fun buildNative(
        context: Context,
        extras: Bundle,
        nb: NotificationCompat.Builder,
        segments: List<SegmentData>,
        points: List<PointData>,
        trackerIcon: Bitmap?,
        ended: Boolean
    ): NotificationCompat.Builder {
        val psClass = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle")
        val progressStyle = psClass.getConstructor().newInstance()

        psClass.getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
            .invoke(progressStyle, boolean(extras, PTConstants.PT_STYLED_BY_PROGRESS, def = false))
        psClass.getMethod("setProgress", Int::class.javaPrimitiveType)
            .invoke(progressStyle, extras.getString(PTConstants.PT_PROGRESS)?.toIntOrNull() ?: 0)

        if (segments.isNotEmpty()) {
            val segClass = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle\$Segment")
            val segCtor = segClass.getConstructor(Int::class.javaPrimitiveType)
            val segSetColor = segClass.getMethod("setColor", Int::class.javaPrimitiveType)
            val segList = segments.map { seg ->
                segCtor.newInstance(seg.length).also { s -> seg.color?.let { segSetColor.invoke(s, it) } }
            }
            psClass.getMethod("setProgressSegments", List::class.java).invoke(progressStyle, segList)
        }
        if (points.isNotEmpty()) {
            val ptClass = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle\$Point")
            val ptCtor = ptClass.getConstructor(Int::class.javaPrimitiveType)
            val ptSetColor = ptClass.getMethod("setColor", Int::class.javaPrimitiveType)
            val ptList = points.map { pt ->
                ptCtor.newInstance(pt.position).also { p -> pt.color?.let { ptSetColor.invoke(p, it) } }
            }
            psClass.getMethod("setProgressPoints", List::class.java).invoke(progressStyle, ptList)
        }

        trackerIcon?.let {
            psClass.getMethod("setProgressTrackerIcon", IconCompat::class.java)
                .invoke(progressStyle, IconCompat.createWithBitmap(it))
        }
        bitmap(context, extras.getString(PTConstants.PT_PROGRESS_START_ICON))?.let {
            psClass.getMethod("setProgressStartIcon", IconCompat::class.java)
                .invoke(progressStyle, IconCompat.createWithBitmap(it))
        }
        bitmap(context, extras.getString(PTConstants.PT_PROGRESS_END_ICON))?.let {
            psClass.getMethod("setProgressEndIcon", IconCompat::class.java)
                .invoke(progressStyle, IconCompat.createWithBitmap(it))
        }

        // setStyle(Style) exists in the baseline core; ProgressStyle is-a Style at runtime.
        nb.setStyle(progressStyle as NotificationCompat.Style)
        applyNativeChip(extras, nb)

        if (!ended && !"false".equals(extras.getString(PTConstants.PT_PROMOTE), ignoreCase = true)) {
            NotificationCompat.Builder::class.java
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(nb, true)
        }
        return nb
    }

    private fun applyNativeChip(extras: Bundle, nb: NotificationCompat.Builder) {
        when (extras.getString(PTConstants.PT_CHIP_TYPE)?.lowercase()) {
            "text" -> extras.getString(PTConstants.PT_CHIP_TEXT)?.takeIf { it.isNotEmpty() }?.let {
                // setShortCriticalText is androidx.core 1.17.0-only -> reflect (same tier as ProgressStyle).
                NotificationCompat.Builder::class.java
                    .getMethod("setShortCriticalText", String::class.java)
                    .invoke(nb, it)
            }

            "timer", "countdown" -> extras.getString(PTConstants.PT_WHEN)?.toLongOrNull()?.let { whenMs ->
                nb.setWhen(whenMs).setUsesChronometer(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    nb.setChronometerCountDown(boolean(extras, PTConstants.PT_COUNTDOWN, def = false))
                }
            }
        }
    }

    // --- Below API 36 : custom-RemoteViews fallback that mimics the ProgressStyle look ---

    private fun buildFallback(
        context: Context,
        extras: Bundle,
        nb: NotificationCompat.Builder,
        title: String,
        message: String,
        segments: List<SegmentData>,
        points: List<PointData>,
        trackerIcon: Bitmap?
    ): NotificationCompat.Builder {
        val chipText = extras.getString(PTConstants.PT_CHIP_TEXT)

        val big = fallbackView(context, R.layout.pt_progress_fallback, title, message,
            chipText, trackerIcon, segments, points)
        val small = fallbackView(context, R.layout.pt_progress_fallback_collapsed, title, message,
            chipText, trackerIcon, segments, points)

        nb.setCustomContentView(small)
            .setCustomBigContentView(big)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true) // no OS promotion below 16; keep it sticky like a live update
        return nb
    }

    private fun fallbackView(
        context: Context,
        layoutId: Int,
        title: String,
        message: String,
        chipText: String?,
        trackerIcon: Bitmap?,
        segments: List<SegmentData>,
        points: List<PointData>
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, layoutId)
        rv.setTextViewText(R.id.pt_title, Html.fromHtml(title))
        rv.setTextViewText(R.id.pt_message, message)

        if (!chipText.isNullOrEmpty()) {
            rv.setTextViewText(R.id.pt_chip, chipText)
            rv.setViewVisibility(R.id.pt_chip, android.view.View.VISIBLE)
        }
        if (trackerIcon != null) {
            rv.setImageViewBitmap(R.id.pt_tracker, trackerIcon)
            rv.setViewVisibility(R.id.pt_tracker, android.view.View.VISIBLE)
        }

        // Points as dots, segments as weighted colored connectors between them.
        rv.removeAllViews(R.id.pt_progress_container)
        if (points.isNotEmpty()) {
            points.forEachIndexed { i, p ->
                val dot = RemoteViews(context.packageName, R.layout.pt_progress_point)
                dot.setInt(R.id.pt_dot, "setColorFilter", p.color ?: COLOR_POINT_DEFAULT)
                rv.addView(R.id.pt_progress_container, dot)
                if (i < segments.size) {
                    rv.addView(R.id.pt_progress_container, segmentView(context, segments[i]))
                }
            }
        } else {
            // No points: render a continuous weighted segmented bar.
            segments.forEach { rv.addView(R.id.pt_progress_container, segmentView(context, it)) }
        }
        return rv
    }

    private fun segmentView(context: Context, seg: SegmentData): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.pt_progress_segment)
        v.setInt(R.id.pt_seg, "setBackgroundColor", seg.color ?: COLOR_INACTIVE)
        return v
    }

    // --- shared parsing ---

    private fun parseSegments(json: String?): List<SegmentData> {
        val out = ArrayList<SegmentData>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(SegmentData(o.optInt("length", 1), colorOrNull(o.optString("color"))))
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse segments", t)
        }
        return out
    }

    private fun parsePoints(json: String?): List<PointData> {
        val out = ArrayList<PointData>()
        if (json.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(PointData(o.optInt("position", 0), colorOrNull(o.optString("color"))))
            }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to parse points", t)
        }
        return out
    }

    private fun bitmap(context: Context, url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            renderer.templateMediaManager.getNotificationBitmap(url, false, context)?.let { squareCrop(it) }
        } catch (t: Throwable) {
            PTLog.verbose("pt_progress: failed to load icon $url", t)
            null
        }
    }

    /**
     * Center-crops to a square so a rectangular source photo isn't stretched — the tracker/start/end
     * icons are rendered in a square slot both on the native bar and in the fallback ImageView.
     */
    private fun squareCrop(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        if (src.width == size && src.height == size) return src
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return try {
            Bitmap.createBitmap(src, x, y, size, size)
        } catch (t: Throwable) {
            src
        }
    }

    private fun boolean(extras: Bundle, key: String, def: Boolean): Boolean =
        extras.getString(key)?.let { "true".equals(it, ignoreCase = true) } ?: def

    private fun colorOrNull(hex: String?): Int? =
        if (hex.isNullOrEmpty()) null else try { Color.parseColor(hex) } catch (t: Throwable) { null }

    private fun parseColor(hex: String?): Int =
        colorOrNull(hex) ?: Color.parseColor(PTConstants.PT_COLOUR_GREY)
}
