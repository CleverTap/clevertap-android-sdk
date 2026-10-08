package com.clevertap.android.pushtemplates.styles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils

/**
 * Draws the pre-16 milestone track (segments + points + point titles) into a Bitmap.
 *
 * RemoteViews cannot set `layout_weight` at runtime, so a row of views can only give every segment
 * the same width. Drawing the track ourselves lets segment widths follow their `length` and points
 * sit at their `position`, on the same scale Android 16's native ProgressStyle uses
 * (track total = sum of segment lengths).
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

    data class SegmentRect(val left: Float, val right: Float, val color: Int)
    data class Dot(val cx: Float, val color: Int, val title: String?)
    data class Layout(
        val width: Int,
        val height: Int,
        val trackY: Float,
        val trackThickness: Float,
        val dotRadius: Float,
        val segments: List<SegmentRect>,
        val dots: List<Dot>,
        val titleBaseline: Float
    )

    private const val DOT_DP = 14f
    private const val TRACK_DP = 3f
    private const val TITLE_SP = 9f
    private const val TITLE_GAP_DP = 3f

    /** Track total, matching native ProgressStyle: the segment lengths' sum, else [fallbackTotal]. */
    fun total(segments: List<ProgressPayloadParser.SegmentData>, fallbackTotal: Int): Int =
        segments.sumOf { it.length.coerceAtLeast(0) }.takeIf { it > 0 } ?: fallbackTotal.coerceAtLeast(1)

    fun layout(
        spec: Spec,
        segments: List<ProgressPayloadParser.SegmentData>,
        points: List<ProgressPayloadParser.PointData>,
        total: Int,
        titlePaint: TextPaint
    ): Layout {
        val dotRadius = DOT_DP * spec.density / 2f
        val trackThickness = TRACK_DP * spec.density
        val hasTitles = points.any { !it.title.isNullOrEmpty() }
        val titleHeight = if (hasTitles) {
            TITLE_GAP_DP * spec.density + (titlePaint.fontMetrics.descent - titlePaint.fontMetrics.ascent)
        } else 0f
        val width = spec.widthPx.coerceAtLeast(1)
        val height = Math.ceil((dotRadius * 2 + titleHeight).toDouble()).toInt().coerceAtLeast(1)

        // Inset the usable track by the dot radius so dots at 0 and at the total are not clipped.
        val start = dotRadius
        val usable = (width - 2 * dotRadius).coerceAtLeast(1f)
        fun x(value: Int): Float = start + usable * (value.coerceIn(0, total).toFloat() / total)

        var acc = 0
        val segmentRects = segments.map { seg ->
            val left = x(acc)
            acc += seg.length.coerceAtLeast(0)
            SegmentRect(left, x(acc), seg.color ?: spec.trackColor)
        }
        val dots = points.map { Dot(x(it.position), it.color ?: spec.pointColor, it.title?.takeIf { t -> t.isNotEmpty() }) }
        val titleBaseline = dotRadius * 2 + TITLE_GAP_DP * spec.density - titlePaint.fontMetrics.ascent
        return Layout(width, height, dotRadius, trackThickness, dotRadius, segmentRects, dots, titleBaseline)
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
        total: Int
    ): Bitmap {
        val titlePaint = titlePaint(spec)
        val l = layout(spec, segments, points, total, titlePaint)
        val bitmap = Bitmap.createBitmap(l.width, l.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val top = l.trackY - l.trackThickness / 2
        val bottom = l.trackY + l.trackThickness / 2
        for (seg in l.segments) {
            if (seg.right <= seg.left) continue
            paint.color = seg.color
            canvas.drawRect(seg.left, top, seg.right, bottom, paint)
        }
        for (dot in l.dots) {
            paint.color = dot.color
            canvas.drawCircle(dot.cx, l.trackY, l.dotRadius, paint)
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
