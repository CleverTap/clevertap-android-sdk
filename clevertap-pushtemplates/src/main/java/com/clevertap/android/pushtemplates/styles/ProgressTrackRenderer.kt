package com.clevertap.android.pushtemplates.styles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils

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
 * Colors follow the native palette rules: a segment/point without its own color uses the theme
 * accent ([Spec.defaultColor]), and every color is nudged lighter/darker until it has 3:1 contrast
 * with the card background ([ensureContrast]), so payload colors stay visible in light and dark mode.
 *
 * The geometry ([layout]) is a pure function so it can be unit-tested without real drawing.
 */
internal object ProgressTrackRenderer {

    data class Spec(
        val widthPx: Int,
        val density: Float,
        val scaledDensity: Float,
        /** Theme accent, for segments/points without their own color (native: primary accent). */
        val defaultColor: Int,
        /** Point title text color (native: secondary text). */
        val titleColor: Int,
        /** The card background the track is drawn on, used for the contrast check. */
        val backgroundColor: Int
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
    private const val TITLE_SPACING_DP = 6f // min space between neighbouring titles
    private const val FADED_OPACITY = 0.5f // native NotificationProgressBar.FADED_OPACITY
    private const val MIN_CONTRAST = 3.0 // native ProgressStyle.sanitizeProgressColor
    private const val DARK_BG_LUMINANCE = 0.17912878474 // native Notification.Builder.isColorDark

    private const val MAX_SEGMENTS = 10 // native MAX_PROGRESS_SEGMENT_LIMIT
    private const val MAX_POINTS = 4 // native MAX_PROGRESS_POINT_LIMIT
    private const val DEFAULT_TOTAL = 100 // native DEFAULT_PROGRESS_MAX

    /** The segments and points actually drawn, and the track total they sit on. */
    data class Track(
        val segments: List<ProgressPayloadParser.SegmentData>,
        val points: List<ProgressPayloadParser.PointData>,
        val total: Int
    )

    /**
     * Applies native ProgressStyle's rules (Notification.ProgressStyle.createProgressModel, API 36) so
     * one payload draws the same track on every tier:
     * - segments with length <= 0 are dropped; the total is the sum of the rest;
     * - no segments (or a total that overflows) -> one default-colored segment of 100;
     * - more than 10 segments -> one segment of the same total, keeping the color only when all
     *   segments share it;
     * - points at or before 0 or at or past the total are dropped, and only the first 4 are kept;
     *   of points sharing a position the last one wins (native keys them by position).
     */
    fun nativeTrack(
        segments: List<ProgressPayloadParser.SegmentData>,
        points: List<ProgressPayloadParser.PointData>
    ): Track {
        val valid = segments.filter { it.length > 0 }
        val sum = valid.sumOf { it.length.toLong() }
        val (segs, total) = when {
            valid.isEmpty() || sum > Int.MAX_VALUE ->
                listOf(ProgressPayloadParser.SegmentData(DEFAULT_TOTAL, null)) to DEFAULT_TOTAL
            valid.size > MAX_SEGMENTS -> {
                val first = valid.first().color
                listOf(ProgressPayloadParser.SegmentData(sum.toInt(), first.takeIf { valid.all { s -> s.color == first } })) to sum.toInt()
            }
            else -> valid to sum.toInt()
        }
        val kept = points.filter { it.position in 1 until total }.take(MAX_POINTS)
        return Track(segs, kept.associateBy { it.position }.values.toList(), total)
    }

    /** Native fade: same color at half its alpha. */
    fun faded(color: Int): Int =
        Color.argb((Color.alpha(color) * FADED_OPACITY + 0.5f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    /**
     * Native ProgressStyle.sanitizeProgressColor (Notification.Builder.ensureColorContrast, API 36):
     * keeps the hue and moves only the lightness, just enough to reach 3:1 contrast with [bg]. Like
     * native, a dark background (luminance <= 0.179) lightens the color in HSL; a light one darkens
     * it in LAB. Each is a 15-step binary search, as in ContrastColorUtil.
     */
    fun ensureContrast(color: Int, bg: Int): Int {
        val opaqueBg = ColorUtils.setAlphaComponent(bg, 255)
        fun meets(c: Int) = ColorUtils.calculateContrast(c, opaqueBg) > MIN_CONTRAST
        val opaque = ColorUtils.setAlphaComponent(color, 255)
        if (ColorUtils.calculateContrast(opaque, opaqueBg) >= MIN_CONTRAST) return color

        val adjusted = if (ColorUtils.calculateLuminance(opaqueBg) <= DARK_BG_LUMINANCE) {
            // ContrastColorUtil.findContrastColorAgainstDark: raise the HSL lightness.
            val hsl = FloatArray(3).also { ColorUtils.colorToHSL(opaque, it) }
            var low = hsl[2]
            var high = 1f
            for (i in 0 until 15) {
                if (high - low <= 0.00001f) break
                hsl[2] = (low + high) / 2
                if (meets(ColorUtils.HSLToColor(hsl))) high = hsl[2] else low = hsl[2]
            }
            hsl[2] = high
            ColorUtils.HSLToColor(hsl)
        } else {
            // ContrastColorUtil.findContrastColor: lower the LAB lightness.
            val lab = DoubleArray(3).also { ColorUtils.colorToLAB(opaque, it) }
            var low = 0.0
            var high = lab[0]
            for (i in 0 until 15) {
                if (high - low <= 0.00001) break
                val l = (low + high) / 2
                if (meets(ColorUtils.LABToColor(l, lab[1], lab[2]))) low = l else high = l
            }
            ColorUtils.LABToColor(low, lab[1], lab[2])
        }
        return ColorUtils.setAlphaComponent(adjusted, Color.alpha(color))
    }

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
            val color = ensureContrast(seg.color ?: spec.defaultColor, spec.backgroundColor)
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
            val color = ensureContrast(it.color ?: spec.defaultColor, spec.backgroundColor)
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
        drawTitles(canvas, l, titlePaint, TITLE_SPACING_DP * spec.density)
        return bitmap
    }

    /** Horizontal room a title may use: [left, right] on the bitmap. */
    data class TitleSlot(val left: Float, val right: Float)

    /**
     * Gives each title a slot so neighbours never overlap and keep [gap] between them. [centers]
     * (the dots, sorted) and [widths] (each title's natural width) line up by index.
     *
     * Each title starts centered under its dot (kept on-canvas). Where two collide they first slide
     * apart, each still reaching its own dot; if that is not enough they are trimmed (ellipsized
     * later), again only as far as each still reaches its dot; dots closer than [gap] split at the
     * midpoint. So two milestones close together (say at 40% and 45%) shift apart instead of drawing
     * over each other.
     */
    fun titleSlots(centers: List<Float>, widths: List<Float>, canvasWidth: Float, gap: Float): List<TitleSlot> {
        val n = centers.size
        val left = FloatArray(n)
        val right = FloatArray(n)
        for (i in 0 until n) {
            val w = widths[i].coerceIn(0f, canvasWidth)
            left[i] = (centers[i] - w / 2).coerceIn(0f, canvasWidth - w)
            right[i] = left[i] + w
        }
        for (i in 0 until n - 1) {
            val j = i + 1
            var overflow = right[i] + gap - left[j]
            if (overflow <= 0) continue

            // 1. Slide apart: the left title not past its own left neighbour, neither past its dot.
            val floor = if (i == 0) 0f else right[i - 1] + gap
            val slideLeft = minOf(overflow, (left[i] - maxOf(floor, centers[i] - (right[i] - left[i]))).coerceAtLeast(0f))
            left[i] -= slideLeft; right[i] -= slideLeft; overflow -= slideLeft
            val slideRight = minOf(overflow, (minOf(centers[j], canvasWidth - (right[j] - left[j])) - left[j]).coerceAtLeast(0f))
            left[j] += slideRight; right[j] += slideRight; overflow -= slideRight
            if (overflow <= 0) continue

            // 2. Trim the facing ends, sharing the overflow, each only down to its own dot.
            val capLeft = (right[i] - maxOf(centers[i], left[i])).coerceAtLeast(0f)
            val capRight = (centers[j] - left[j]).coerceAtLeast(0f)
            val trimRight = minOf(capRight, overflow - minOf(capLeft, overflow / 2))
            val trimLeft = minOf(capLeft, overflow - trimRight)
            right[i] -= trimLeft; left[j] += trimRight; overflow -= trimLeft + trimRight
            if (overflow <= 0) continue

            // 3. Dots closer than the gap itself: split at the midpoint.
            val mid = (centers[i] + centers[j]) / 2
            right[i] = minOf(right[i], mid - gap / 2)
            left[j] = maxOf(left[j], mid + gap / 2)
        }
        return (0 until n).map { TitleSlot(left[it], maxOf(left[it], right[it])) }
    }

    /** Draws each title in its [titleSlots] slot, ellipsized to fit, as centered on its dot as the slot allows. */
    private fun drawTitles(canvas: Canvas, l: Layout, paint: TextPaint, gap: Float) {
        val titled = l.dots.filter { it.title != null }.sortedBy { it.cx }
        val slots = titleSlots(titled.map { it.cx }, titled.map { paint.measureText(it.title) }, l.width.toFloat(), gap)
        titled.forEachIndexed { i, dot ->
            val slot = slots[i]
            val text = TextUtils.ellipsize(dot.title, paint, slot.right - slot.left, TextUtils.TruncateAt.END).toString()
            if (text.isEmpty()) return@forEachIndexed
            val w = paint.measureText(text)
            val x = (dot.cx - w / 2).coerceIn(slot.left, maxOf(slot.left, slot.right - w))
            canvas.drawText(text, x, l.titleBaseline, paint)
        }
    }
}
