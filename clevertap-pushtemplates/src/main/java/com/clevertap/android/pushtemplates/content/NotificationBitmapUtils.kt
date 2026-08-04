package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import com.clevertap.android.pushtemplates.PTLog
import kotlin.math.cos
import kotlin.math.sin

internal object NotificationBitmapUtils {

    fun createSolidBitmap(
        bgColor: Int,
        borderColor: Int?,
        width: Int,
        height: Int,
        cornerRadius: Float,
        borderWidth: Float? = null
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        drawRoundRect(canvas, paint, width, height, cornerRadius, borderColor, borderWidth)
        return bitmap
    }

    fun createLinearGradientBitmap(
        color1: Int,
        color2: Int,
        direction: Double,
        width: Int,
        height: Int,
        cornerRadius: Float,
        borderColor: Int? = null,
        borderWidth: Float? = null
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val rad = Math.toRadians(direction)
        val sinA = sin(rad).toFloat()
        val cosA = cos(rad).toFloat()
        val shader = LinearGradient(
            w * (0.5f - 0.5f * sinA), h * (0.5f + 0.5f * cosA),
            w * (0.5f + 0.5f * sinA), h * (0.5f - 0.5f * cosA),
            color1, color2, Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        drawRoundRect(canvas, paint, width, height, cornerRadius, borderColor, borderWidth)
        return bitmap
    }

    fun createRadialBitmap(
        color1: Int,
        color2: Int,
        width: Int,
        height: Int,
        cornerRadius: Float,
        borderColor: Int? = null,
        borderWidth: Float? = null
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val shader = RadialGradient(w / 2f, h / 2f, maxOf(w, h) / 2f, color1, color2, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        drawRoundRect(canvas, paint, width, height, cornerRadius, borderColor, borderWidth)
        return bitmap
    }

    private fun drawRoundRect(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        cornerRadius: Float,
        borderColor: Int?,
        borderWidth: Float?
    ) {
        if (borderColor != null) {
            val strokeWidth = borderWidth ?: (height * BORDER_STROKE_RATIO)
            val inset = strokeWidth / 2f
            val rect = RectF(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        } else {
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    private const val BORDER_STROKE_RATIO = 0.10f

    /**
     * Draws [source] into a new bitmap with rounded corners and an optional border.
     *
     * [cornerRadiusPercent] and [borderWidthPercent] are percentages of the image's shortest side,
     * not absolute pixels. Notification images are supplied by the campaign, so their pixel
     * dimensions vary; a percentage keeps the same payload value looking identical whether the
     * image is 240x180 or 1200x800. This mirrors how in-app notifications size themselves from the
     * `xp`/`yp` payload keys.
     */
    fun applyRoundedBorderToBitmap(
        source: Bitmap,
        cornerRadiusPercent: Float,
        borderColor: Int?,
        borderWidthPercent: Float?
    ): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        if (cornerRadiusPercent <= 0f && borderColor == null) return source

        val minDimension = minOf(width, height)

        val strokeWidth =
            if (borderColor != null) resolveBorderWidthPx(minDimension, borderWidthPercent) else 0f
        val safeRadius = resolveCornerRadiusPx(minDimension, cornerRadiusPercent)

        val half = strokeWidth / 2f
        val strokeRect = RectF(half, half, width - half, height - half)
        val adjustedRadius = (safeRadius - half).coerceAtLeast(0f)

        PTLog.debug(
            "Image border on ${width}x$height bitmap: corner radius $cornerRadiusPercent% -> " +
                    "${safeRadius}px, border width ${borderWidthPercent ?: DEFAULT_BORDER_WIDTH_PERCENT}% -> ${strokeWidth}px"
        )

        val output = createBitmap(width, height)
        val canvas = Canvas(output)

        // Draw the full image into the full bitmap area — BitmapShader maps canvas coords 1:1 to
        // source coords, so using an inset rect would silently drop edge pixels from the source.
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            safeRadius, safeRadius, imagePaint
        )

        // Draw the border stroke on top, inset by half the stroke width so it stays within the bitmap.
        if (borderColor != null && strokeWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = borderColor
            }
            canvas.drawRoundRect(strokeRect, adjustedRadius, adjustedRadius, borderPaint)
        }

        return output
    }

    private const val MAX_BORDER_RATIO = 0.25f

    // Percentage equivalents of the ratios above, used by applyRoundedBorderToBitmap
    internal const val DEFAULT_BORDER_WIDTH_PERCENT = BORDER_STROKE_RATIO * 100f
    internal const val MAX_BORDER_WIDTH_PERCENT = MAX_BORDER_RATIO * 100f

    // 50% of the shortest side is a full pill; anything beyond that has no visible effect
    internal const val MAX_CORNER_RADIUS_PERCENT = 50f

    /**
     * Resolves [cornerRadiusPercent] against the image's shortest side.
     *
     * Kept separate from the drawing code so the conversion can be asserted directly; Canvas
     * operations are no-ops under Robolectric's legacy graphics mode, so a test that only inspects
     * the output bitmap cannot tell a percentage from a raw pixel count.
     */
    internal fun resolveCornerRadiusPx(minDimension: Int, cornerRadiusPercent: Float): Float =
        minDimension * cornerRadiusPercent.coerceIn(0f, MAX_CORNER_RADIUS_PERCENT) / 100f

    /**
     * Resolves [borderWidthPercent] against the image's shortest side, falling back to
     * [DEFAULT_BORDER_WIDTH_PERCENT] when the payload omits it.
     *
     * Clamped at both ends: a negative payload value must not produce a negative stroke, which
     * would both drop the border and push the draw rect outside the bitmap bounds.
     */
    internal fun resolveBorderWidthPx(minDimension: Int, borderWidthPercent: Float?): Float {
        val percent = (borderWidthPercent ?: DEFAULT_BORDER_WIDTH_PERCENT)
            .coerceIn(0f, MAX_BORDER_WIDTH_PERCENT)
        return minDimension * percent / 100f
    }
}
