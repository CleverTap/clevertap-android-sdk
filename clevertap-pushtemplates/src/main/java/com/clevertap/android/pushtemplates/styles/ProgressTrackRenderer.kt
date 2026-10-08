package com.clevertap.android.pushtemplates.styles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils

/**
 * Draws the pre-16 milestone track (segments + points + point titles + progress) into a Bitmap.
 *
 * RemoteViews cannot set `layout_weight` at runtime, so a row of views can only give every segment
 * the same width. Drawing the track ourselves lets segment widths follow their `length` and points
 * sit at their `position`, on the same scale Android 16's native ProgressStyle uses
 * (track total = sum of segment lengths).
 *
 * Progress is shown the way the native bar shows it (NotificationProgressBar, API 36):
 * - the tracker icon sits on the track at the progress position;
 * - when styled by progress, everything ahead of the progress is faded (half alpha, thinner
 *   segments), with a small gap at the progress position when there is no tracker to cover it.
 *
 * The geometry ([layout]) is a pure function so it can be unit-tested without real drawing.
 */
internal object ProgressTrackRenderer {

    data class Spec(
        val widthPx: Int,
        val density: Float,
        val scaledDensity: Float,
        val trackColor: Int,
        val pointColor: Int,
        val titleColor: Int
    )

    data class SegmentRect(val left: Float, val right: Float, val color: Int, val faded: Boolean = false)
    data class Dot(val cx: Float, val color: Int, val title: String?)
    data class Layout(
        val width: Int,
        val height: Int,
        val trackY: Float,
        val trackThickness: Float,
        val fadedTrackThickness: Float,
        val dotRadius: Float,
        val segments: List<SegmentRect>,
        val dots: List<Dot>,
        val titleBaseline: Float,
        val progressX: Float,
        /** Tracker bounds on the track; null when there is no tracker icon. */
        val tracker: RectF?
    )

    private const val DOT_DP = 14f
    private const val TRACK_DP = 3f
    private const val FADED_TRACK_DP = 1f // native: 2dp faded vs 6dp segments, same 1/3 ratio
    private const val PROGRESS_GAP_DP = 4f // native segSeg gap, used at the progress when no tracker
    private const val TRACKER_DP = 20f // native tracker height
    private const val TITLE_SP = 9f
    private const val TITLE_GAP_DP = 3f
    private const val FADED_OPACITY = 0.5f // native NotificationProgressBar.FADED_OPACITY

    /** Track total, matching native ProgressStyle: the segment lengths' sum, else [fallbackTotal]. */
    fun total(segments: List<ProgressPayloadParser.SegmentData>, fallbackTotal: Int): Int =
        segments.sumOf { it.length.coerceAtLeast(0) }.takeIf { it > 0 } ?: fallbackTotal.coerceAtLeast(1)

    /** Native fade: same color at half its alpha. */
    fun faded(color: Int): Int =
        Color.argb((Color.alpha(color) * FADED_OPACITY + 0.5f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    fun layout(
        spec: Spec,
        segments: List<ProgressPayloadParser.SegmentData>,
        points: List<ProgressPayloadParser.PointData>,
        total: Int,
        progress: Int,
        styledByProgress: Boolean,
        hasTracker: Boolean,
        titlePaint: TextPaint
    ): Layout {
        val dotRadius = DOT_DP * spec.density / 2f
        val trackThickness = TRACK_DP * spec.density
        val trackerSize = if (hasTracker) TRACKER_DP * spec.density else 0f
        // The track runs through the middle of the tallest thing on it (dot or tracker).
        val trackY = maxOf(dotRadius, trackerSize / 2f)
        val hasTitles = points.any { !it.title.isNullOrEmpty() }
        val titleHeight = if (hasTitles) {
            TITLE_GAP_DP * spec.density + (titlePaint.fontMetrics.descent - titlePaint.fontMetrics.ascent)
        } else 0f
        val width = spec.widthPx.coerceAtLeast(1)
        val height = Math.ceil((trackY * 2 + titleHeight).toDouble()).toInt().coerceAtLeast(1)

        // Inset the usable track by the dot radius so dots at 0 and at the total are not clipped.
        val start = dotRadius
        val usable = (width - 2 * dotRadius).coerceAtLeast(1f)
        fun x(value: Int): Float = start + usable * (value.coerceIn(0, total).toFloat() / total)

        val clampedProgress = progress.coerceIn(0, total)
        val progressX = x(clampedProgress)
        val fadeAhead = styledByProgress && clampedProgress < total
        // Without a tracker, native leaves a gap before the progress so the split is visible.
        val progressGap = if (hasTracker) 0f else PROGRESS_GAP_DP * spec.density

        var acc = 0
        val segmentRects = ArrayList<SegmentRect>(segments.size + 1)
        for (seg in segments) {
            val left = x(acc)
            acc += seg.length.coerceAtLeast(0)
            val right = x(acc)
            val color = seg.color ?: spec.trackColor
            when {
                !fadeAhead || right <= progressX -> segmentRects.add(SegmentRect(left, right, color))
                left >= progressX -> segmentRects.add(SegmentRect(left, right, faded(color), faded = true))
                else -> { // the progress falls inside this segment: split it there
                    segmentRects.add(SegmentRect(left, maxOf(left, progressX - progressGap), color))
                    segmentRects.add(SegmentRect(progressX, right, faded(color), faded = true))
                }
            }
        }
        val dots = points.map {
            val color = it.color ?: spec.pointColor
            Dot(x(it.position), if (fadeAhead && it.position > clampedProgress) faded(color) else color,
                it.title?.takeIf { t -> t.isNotEmpty() })
        }
        // Tracker centered on the progress, kept fully on the canvas at both ends.
        val tracker = if (hasTracker) {
            val left = (progressX - trackerSize / 2f).coerceIn(0f, (width - trackerSize).coerceAtLeast(0f))
            val top = trackY - trackerSize / 2f
            RectF(left, top, left + trackerSize, top + trackerSize)
        } else null
        val titleBaseline = trackY * 2 + TITLE_GAP_DP * spec.density - titlePaint.fontMetrics.ascent
        return Layout(width, height, trackY, trackThickness, FADED_TRACK_DP * spec.density, dotRadius,
            segmentRects, dots, titleBaseline, progressX, tracker)
    }

    fun titlePaint(spec: Spec) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TITLE_SP * spec.scaledDensity
        color = spec.titleColor
        textAlign = Paint.Align.LEFT
    }

    fun render(
        spec: Spec,
        segments: List<ProgressPayloadParser.SegmentData>,
        points: List<ProgressPayloadParser.PointData>,
        total: Int,
        progress: Int,
        styledByProgress: Boolean,
        tracker: Bitmap?
    ): Bitmap {
        val titlePaint = titlePaint(spec)
        val l = layout(spec, segments, points, total, progress, styledByProgress, tracker != null, titlePaint)
        val bitmap = Bitmap.createBitmap(l.width, l.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for (seg in l.segments) {
            if (seg.right <= seg.left) continue
            val half = (if (seg.faded) l.fadedTrackThickness else l.trackThickness) / 2
            paint.color = seg.color
            canvas.drawRect(seg.left, l.trackY - half, seg.right, l.trackY + half, paint)
        }
        for (dot in l.dots) {
            paint.color = dot.color
            canvas.drawCircle(dot.cx, l.trackY, l.dotRadius, paint)
        }
        // Tracker last so it sits on top of the track and dots, like the native tracker.
        if (tracker != null && l.tracker != null) {
            paint.color = Color.BLACK // opaque so the bitmap is drawn at full alpha
            canvas.drawBitmap(tracker, null, l.tracker, paint)
        }
        drawTitles(canvas, l, titlePaint)
        return bitmap
    }

    /**
     * Centers each title under its dot, ellipsized to the distance to its nearest neighbour. Titles
     * at the ends may shift inward to stay on-canvas.
     */
    private fun drawTitles(canvas: Canvas, l: Layout, paint: TextPaint) {
        val sorted = l.dots.sortedBy { it.cx }
        sorted.forEachIndexed { i, dot ->
            val title = dot.title ?: return@forEachIndexed
            val leftGap = if (i == 0) Float.MAX_VALUE else dot.cx - sorted[i - 1].cx
            val rightGap = if (i == sorted.lastIndex) Float.MAX_VALUE else sorted[i + 1].cx - dot.cx
            val maxWidth = minOf(leftGap, rightGap, l.width.toFloat()).coerceAtLeast(0f)
            val text = TextUtils.ellipsize(title, paint, maxWidth, TextUtils.TruncateAt.END).toString()
            if (text.isEmpty()) return@forEachIndexed
            val w = paint.measureText(text)
            val x = (dot.cx - w / 2).coerceIn(0f, (l.width - w).coerceAtLeast(0f))
            canvas.drawText(text, x, l.titleBaseline, paint)
        }
    }
}
