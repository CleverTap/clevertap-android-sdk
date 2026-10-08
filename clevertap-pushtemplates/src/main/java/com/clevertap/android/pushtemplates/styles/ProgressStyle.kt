package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.ProgressTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.content.PROGRESS_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants
import org.json.JSONArray

// Parsed segment/point models live in the (unit-tested) ProgressPayloadParser.
private typealias SegmentData = ProgressPayloadParser.SegmentData
private typealias PointData = ProgressPayloadParser.PointData

/**
 * Progress-centric template (`pt_progress`).
 *
 * Two rendering tiers, gated on the OS:
 * - **Android 16+ (API 36):** native `NotificationCompat.ProgressStyle` — segments, points,
 *   tracker icon, status chip, and promotion (`setRequestPromotedOngoing`). Invoked via reflection
 *   (see [buildNative]) so the SDK needs no build-time dependency on androidx.core 1.17.0.
 *   This is a base style with NO custom RemoteViews, which is what makes promotion possible.
 * - **Below API 36:** a custom-RemoteViews fallback for the EXPANDED view that mimics the same look —
 *   title/message and a progress row: a bitmap-drawn milestone track (segment widths follow their
 *   length, points sit at their position, the tracker rides it at the progress) or a plain bar with
 *   the tracker beside the title. The COLLAPSED view is left to the system's standard template
 *   (small icon, title, time, message), matching what Android 16 shows for a collapsed
 *   ProgressStyle. Not promotable (promotion is a 16+ OS feature), but kept ongoing so it behaves
 *   like a live update.
 *
 * Both tiers read the same `pt_progress_*` contract. On the native tier the track total is the sum
 * of the segment lengths (there is no separate max), so `pt_progress` and point positions must be on
 * that scale; `pt_progress_max` applies to the fallback tier only. `pt_progress_indeterminate` applies
 * to both tiers, and only when there are no segments/points.
 */
internal class ProgressStyle(
    private val data: ProgressTemplateData,
    private val renderer: TemplateRenderer
) {

    companion object {
        // Android 16 (Baklava) introduced Notification.ProgressStyle + promotion.
        private const val API_PROGRESS_STYLE = 36
        // Fallback-only theme colors for the milestone track bitmap, below API 31 (no Material You
        // system palette). The card text uses the system notification text appearances instead.
        private const val COLOR_BG_LIGHT = 0xFFFFFFFF.toInt()
        private const val COLOR_BG_DARK = 0xFF303030.toInt()
        private const val COLOR_TITLE_LIGHT = 0x8A000000.toInt() // secondary text on light
        private const val COLOR_TITLE_DARK = 0xB3FFFFFF.toInt() // secondary text on dark
        // Milestone track bitmap sizing (fallback only).
        private const val TRACK_SIDE_MARGINS_DP = 60f // shade margins + card padding, both sides
        private const val TRACK_ICON_DP = 22f // a start/end icon (16dp) + its 6dp gap
        private const val MAX_TRACK_WIDTH_PX = 1080 // keeps the bitmap well under the RemoteViews limits
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
        // Segments/points (incl. point titles) are parsed once in TemplateDataFactory.
        val segments = data.segments
        val points = data.points
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
        // The 1.17.0 APIs are looked up once per process (NativeProgressApi); null means unavailable,
        // which is logged once there, so this check costs nothing on later renders.
        val api = if (Build.VERSION.SDK_INT >= API_PROGRESS_STYLE) NativeProgressApi.methods else null
        val nativeOk = api != null &&
            runCatching { buildNative(api, context, extras, nb, segments, points, trackerIcon, ended) }
                .onFailure { PTLog.verbose("pt_progress: native ProgressStyle failed, using fallback", it) }
                .isSuccess
        if (!nativeOk) {
            buildFallback(context, extras, nb, title, message, segments, points, trackerIcon, ended)
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

    /** Adds action buttons from the standard wzrk_acts payload as system actions (all API levels). */
    private fun applyActionButtons(context: Context, extras: Bundle, notificationId: Int, nb: NotificationCompat.Builder) {
        val actions = extras.getString(Constants.WZRK_ACTIONS)
            ?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return
        if (actions.length() == 0) return
        renderer.actionButtons = renderer.getActionButtons(context, extras, notificationId, actions)
        // pt_progress renders via native ProgressStyle (16+) or DecoratedCustomViewStyle (fallback),
        // so standard addAction() buttons show on EVERY API level. That's unlike the RemoteViews
        // templates, which draw buttons inside their own layout and so gate via ActionButtonsHandler
        // at API 31+. Attaching directly here means wzrk_acts isn't silently dropped on API 23–30.
        renderer.setActionButtons(context, extras, notificationId, nb, null)
    }

    // --- Android 16+ : native ProgressStyle + promotion (via reflection) ---
    //
    // NotificationCompat.ProgressStyle + the promotion/chip builder methods only exist in
    // androidx.core 1.17.0. We invoke them REFLECTIVELY so this SDK compiles WITHOUT a build-time
    // dependency on 1.17.0 (no compileOnly / resolutionStrategy force needed): the host app supplies
    // whatever androidx.core it ships. The reflected handles are looked up once and cached in
    // NativeProgressApi; if that runtime core is < 1.17.0 they are absent and builderFromStyle uses
    // the RemoteViews path. Everything already present in the baseline core (IconCompat,
    // setStyle/Style, setWhen/chronometer) is called directly and type-safely.

    private fun buildNative(
        api: NativeProgressApi.Methods,
        context: Context,
        extras: Bundle,
        nb: NotificationCompat.Builder,
        segments: List<SegmentData>,
        points: List<PointData>,
        trackerIcon: Bitmap?,
        ended: Boolean
    ): NotificationCompat.Builder {
        // A new ProgressStyle (and Segment/Point) per notification; only the reflective handles are cached.
        val progressStyle = api.styleCtor.newInstance()

        api.setStyledByProgress.invoke(progressStyle, boolean(extras, PTConstants.PT_STYLED_BY_PROGRESS, def = false))
        // Native ProgressStyle has no max: the track total is the sum of segment lengths, so pt_progress
        // must be on that scale. pt_progress_max is fallback-only here.
        api.setProgress.invoke(progressStyle, extras.getString(PTConstants.PT_PROGRESS)?.toIntOrNull() ?: 0)
        // Same either/or rule as the fallback: indeterminate only applies to a plain bar, never to a
        // milestone (segments/points) indicator.
        api.setProgressIndeterminate.invoke(progressStyle, data.indeterminate && !data.isSegmented)

        if (segments.isNotEmpty()) {
            val segList = segments.map { seg ->
                api.segmentCtor.newInstance(seg.length).also { s -> seg.color?.let { api.segmentSetColor.invoke(s, it) } }
            }
            api.setProgressSegments.invoke(progressStyle, segList)
        }
        if (points.isNotEmpty()) {
            val ptList = points.map { pt ->
                api.pointCtor.newInstance(pt.position).also { p -> pt.color?.let { api.pointSetColor.invoke(p, it) } }
            }
            api.setProgressPoints.invoke(progressStyle, ptList)
        }

        trackerIcon?.let { api.setProgressTrackerIcon.invoke(progressStyle, IconCompat.createWithBitmap(it)) }
        bitmap(context, extras.getString(PTConstants.PT_PROGRESS_START_ICON))?.let {
            api.setProgressStartIcon.invoke(progressStyle, IconCompat.createWithBitmap(it))
        }
        bitmap(context, extras.getString(PTConstants.PT_PROGRESS_END_ICON))?.let {
            api.setProgressEndIcon.invoke(progressStyle, IconCompat.createWithBitmap(it))
        }

        // setStyle(Style) exists in the baseline core; ProgressStyle is-a Style at runtime.
        nb.setStyle(progressStyle as NotificationCompat.Style)
        applyNativeChip(api, extras, nb)

        if (!ended && !"false".equals(extras.getString(PTConstants.PT_PROMOTE), ignoreCase = true)) {
            api.setRequestPromotedOngoing.invoke(nb, true)
        }
        return nb
    }

    private fun applyNativeChip(api: NativeProgressApi.Methods, extras: Bundle, nb: NotificationCompat.Builder) {
        when (extras.getString(PTConstants.PT_CHIP_TYPE)?.lowercase()) {
            // setShortCriticalText is androidx.core 1.17.0-only -> reflected (same tier as ProgressStyle).
            "text" -> extras.getString(PTConstants.PT_CHIP_TEXT)?.takeIf { it.isNotEmpty() }?.let {
                api.setShortCriticalText.invoke(nb, it)
            }

            "timer", "countdown" -> applyChronometer(extras, nb)
        }
    }

    /**
     * Timer / countdown chip: a running chronometer from `pt_when`. Uses only baseline builder APIs,
     * so both tiers share it — on 16+ it drives the status-bar chip and the header, below 16 the
     * header of the system-drawn notification.
     */
    private fun applyChronometer(extras: Bundle, nb: NotificationCompat.Builder) {
        val whenMs = extras.getString(PTConstants.PT_WHEN)?.toLongOrNull() ?: return
        nb.setWhen(whenMs).setUsesChronometer(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nb.setChronometerCountDown(boolean(extras, PTConstants.PT_COUNTDOWN, def = false))
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
        trackerIcon: Bitmap?,
        ended: Boolean
    ): NotificationCompat.Builder {
        // On Android 16 the chip lives in the status bar and goes away once the live update ends
        // (no longer ongoing/promoted), so drop it from the fallback card on the end event too.
        // Like the native tier, pt_chip_text is only used for pt_chip_type=text.
        val textChip = "text".equals(extras.getString(PTConstants.PT_CHIP_TYPE), ignoreCase = true)
        val chipText = if (ended || !textChip) null else extras.getString(PTConstants.PT_CHIP_TEXT)
        val startIcon = bitmap(context, extras.getString(PTConstants.PT_PROGRESS_START_ICON))
        val endIcon = bitmap(context, extras.getString(PTConstants.PT_PROGRESS_END_ICON))

        // Either/or: milestones (the drawn track) when segments/points are present, else a plain
        // bar — indeterminate when flagged, else determinate. Never both. (Segmented ignores the
        // indeterminate flag; a milestone bar is inherently determinate.)
        val segmented = data.isSegmented
        val indeterminate = data.indeterminate && !segmented
        val progress = data.progress ?: 0
        // Determinate max: pt_progress_max, else the summed segment lengths, else 100.
        val progressMax = data.progressMax ?: segments.sumOf { it.length }.takeIf { it > 0 } ?: 100

        val styledByProgress = boolean(extras, PTConstants.PT_STYLED_BY_PROGRESS, def = false)

        val big = fallbackView(context, segmented, indeterminate, title, message, chipText, trackerIcon,
            startIcon, endIcon, segments, points, progress, progressMax, styledByProgress)

        // Only the expanded view is custom. No custom content view is set, so the collapsed view is
        // the system's standard template (small icon, title, time, message) on every API level —
        // the same fields Android 16 shows for a collapsed native ProgressStyle.
        // Timer / countdown chip: there is no status-bar chip below 16, so show the running time in
        // the notification header (Android 16 shows it there too, besides the chip).
        when (extras.getString(PTConstants.PT_CHIP_TYPE)?.lowercase()) {
            "timer", "countdown" -> applyChronometer(extras, nb)
        }

        nb.setCustomBigContentView(big)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(!ended) // sticky like a live update while active; swipeable once ended
        return nb
    }

    private fun fallbackView(
        context: Context,
        segmented: Boolean,
        indeterminate: Boolean,
        title: String,
        message: String,
        chipText: String?,
        trackerIcon: Bitmap?,
        startIcon: Bitmap?,
        endIcon: Bitmap?,
        segments: List<SegmentData>,
        points: List<PointData>,
        progress: Int,
        progressMax: Int,
        styledByProgress: Boolean
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.pt_progress_fallback)
        rv.setTextViewText(R.id.pt_title, Html.fromHtml(title))
        rv.setTextViewText(R.id.pt_message, message)

        if (!chipText.isNullOrEmpty()) {
            rv.setTextViewText(R.id.pt_chip, chipText)
            rv.setViewVisibility(R.id.pt_chip, android.view.View.VISIBLE)
        }
        // Android 16 hides the tracker on an indeterminate bar (there is no position to put it at),
        // so hide it here too to match. On the milestone track the tracker is drawn on the track at
        // the progress position instead (see below), so it is not shown up here as well.
        if (trackerIcon != null && !indeterminate && !segmented) {
            rv.setImageViewBitmap(R.id.pt_tracker, trackerIcon)
            rv.setViewVisibility(R.id.pt_tracker, android.view.View.VISIBLE)
        }

        // Start/end icons sit on both sides of the indicator for both the milestone and the plain
        // bar, as on Android 16.
        startIcon?.let {
            rv.setImageViewBitmap(R.id.pt_start_icon, it)
            rv.setViewVisibility(R.id.pt_start_icon, android.view.View.VISIBLE)
        }
        endIcon?.let {
            rv.setImageViewBitmap(R.id.pt_end_icon, it)
            rv.setViewVisibility(R.id.pt_end_icon, android.view.View.VISIBLE)
        }

        if (segmented) {
            // Milestones: show the bitmap-drawn track, hide the plain bar. Same scale as native 16+:
            // the total is the sum of the segment lengths (pt_progress_max only when there are none).
            // Progress shows as on native: the tracker at the progress, and (pt_styled_by_progress)
            // everything ahead of it faded.
            rv.setViewVisibility(R.id.pt_bar, android.view.View.GONE)
            val total = ProgressTrackRenderer.total(segments, progressMax)
            val track = ProgressTrackRenderer.render(
                trackSpec(context, hasStartIcon = startIcon != null, hasEndIcon = endIcon != null),
                segments, points, total, progress, styledByProgress, trackerIcon
            )
            rv.setImageViewBitmap(R.id.pt_progress_track, track)
            rv.setViewVisibility(R.id.pt_progress_track, android.view.View.VISIBLE)
            // TalkBack: the track is a picture, so describe it as a percent like the plain bar.
            val percent = progress.coerceIn(0, total) * 100 / total
            rv.setContentDescription(R.id.pt_progress_track, context.getString(R.string.pt_progress_bar_cd, percent))
        } else {
            // Plain bar: show the bar (indeterminate or determinate); the track stays hidden.
            rv.setViewVisibility(R.id.pt_bar, android.view.View.VISIBLE)
            if (indeterminate) {
                rv.setProgressBar(R.id.pt_bar, 0, 0, true)
                rv.setContentDescription(R.id.pt_bar, context.getString(R.string.pt_progress_indeterminate_cd))
            } else {
                // pt_progress_max is parsed with no lower bound, so a malformed negative value would
                // make coerceIn(0, progressMax) throw ("empty range") and drop the whole push. Clamp
                // the max to at least 1 so a bad payload degrades to an empty bar instead of crashing.
                val safeMax = progressMax.coerceAtLeast(1)
                val clamped = progress.coerceIn(0, safeMax)
                rv.setProgressBar(R.id.pt_bar, safeMax, clamped, false)
                val percent = clamped * 100 / safeMax
                rv.setContentDescription(R.id.pt_bar, context.getString(R.string.pt_progress_bar_cd, percent))
            }
        }
        return rv
    }

    /**
     * Bitmap width for the milestone track: roughly the width the track gets inside the expanded card
     * (screen width minus the shade/card margins and any start/end icons), capped to keep the bitmap
     * small. The ImageView scales it with fitCenter, so a small estimate error never distorts the dots.
     *
     * Colors follow the system light/dark theme like the native palette (Notification.Colors): on
     * API 31+ the Material You system colors (primary accent, surface, on-surface-variant); below
     * that, the notification color as the accent on a plain light/dark card. The bitmap is drawn
     * once per post, so a theme switch shows on the next update.
     */
    private fun trackSpec(context: Context, hasStartIcon: Boolean, hasEndIcon: Boolean): ProgressTrackRenderer.Spec {
        val dm = context.resources.displayMetrics
        val iconsDp = (if (hasStartIcon) TRACK_ICON_DP else 0f) + (if (hasEndIcon) TRACK_ICON_DP else 0f)
        val estimated = dm.widthPixels - ((TRACK_SIDE_MARGINS_DP + iconsDp) * dm.density).toInt()
        val dark = Utils.isDarkMode(context)
        val (accent, background, titleColor) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Material 3 roles: primary = accent1 tone 40/80, surface container high ≈ neutral1
            // tone 90/20, on-surface-variant = neutral2 tone 30/80.
            fun c(id: Int) = context.getColor(id)
            if (dark) Triple(c(android.R.color.system_accent1_200), c(android.R.color.system_neutral1_800), c(android.R.color.system_neutral2_200))
            else Triple(c(android.R.color.system_accent1_600), c(android.R.color.system_neutral1_100), c(android.R.color.system_neutral2_700))
        } else {
            Triple(parseColor(renderer.smallIconColour),
                if (dark) COLOR_BG_DARK else COLOR_BG_LIGHT,
                if (dark) COLOR_TITLE_DARK else COLOR_TITLE_LIGHT)
        }
        @Suppress("DEPRECATION") // scaledDensity keeps the titles in step with the user's font size
        return ProgressTrackRenderer.Spec(
            widthPx = estimated.coerceIn(1, MAX_TRACK_WIDTH_PX),
            density = dm.density,
            scaledDensity = dm.scaledDensity,
            defaultColor = accent,
            titleColor = titleColor,
            backgroundColor = background
        )
    }

    // --- media / helpers ---

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

    private fun parseColor(hex: String?): Int =
        Utils.getColour(hex, PTConstants.PT_COLOUR_GREY)
}
