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
import com.clevertap.android.pushtemplates.PTConstants
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
     * [cornerRadiusDp] is a dp value describing the curve the user should see on screen, so it has to
     * be converted into the source bitmap's pixel space: the radius is baked into the bitmap, and a
     * 1200px-wide image shown at 360dp needs a proportionally larger pixel radius than a 360px one to
     * look the same. [mediaWidthDp] is the on-screen width the media is laid out at, which the caller
     * derives from the device's display metrics.
     *
     * [borderWidthDp] is converted the same way, so a border stays the same visible thickness whatever
     * the image's resolution or aspect ratio.
     */
    fun applyRoundedBorderToBitmap(
        source: Bitmap,
        cornerRadiusDp: Int,
        borderColor: Int?,
        borderWidthDp: Int?,
        mediaWidthDp: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        if (cornerRadiusDp <= 0 && borderColor == null) return source

        val strokeWidth =
            if (borderColor != null) resolveBorderWidthPx(width, height, borderWidthDp, mediaWidthDp)
            else 0f
        val safeRadius = resolveCornerRadiusPx(width, height, cornerRadiusDp, mediaWidthDp)

        val half = strokeWidth / 2f
        val strokeRect = RectF(half, half, width - half, height - half)
        val adjustedRadius = (safeRadius - half).coerceAtLeast(0f)

        PTLog.debug(
            "Media border on ${width}x$height bitmap shown at ${mediaWidthDp}dp: corner radius " +
                    "${cornerRadiusDp}dp -> ${safeRadius}px, border width " +
                    "${borderWidthDp ?: PTConstants.PT_MEDIA_BORDER_WIDTH_DEFAULT}dp -> ${strokeWidth}px"
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

    /**
     * Hard ceiling on the border as a share of the image's shortest side. A dp value is free to be
     * larger than a small image can carry, and a stroke past this point stops reading as a border and
     * starts eating the picture.
     */
    private const val MAX_BORDER_RATIO = 0.25f

    /**
     * Converts [cornerRadiusDp] into a pixel radius in the source bitmap's coordinate space.
     *
     * Kept separate from the drawing code so the conversion can be asserted directly; Canvas
     * operations are no-ops under Robolectric's legacy graphics mode, so a test that only inspects
     * the output bitmap cannot tell one radius from another.
     *
     * The result is capped at half the shortest side. Beyond that the round rect degenerates — the
     * corners meet and further radius has no visible effect — so clamping keeps a large dp value on
     * a small image looking like the pill it asked for instead of drawing outside the shape.
     */
    internal fun resolveCornerRadiusPx(
        bitmapWidth: Int,
        bitmapHeight: Int,
        cornerRadiusDp: Int,
        mediaWidthDp: Float
    ): Float {
        val dp = cornerRadiusDp.coerceIn(0, PTConstants.PT_MEDIA_RADIUS_MAX)
        if (dp == 0 || mediaWidthDp <= 0f || bitmapWidth <= 0) return 0f
        // Bitmap pixels per on-screen dp.
        val pixelsPerDp = bitmapWidth / mediaWidthDp
        val radius = dp * pixelsPerDp
        return radius.coerceAtMost(minOf(bitmapWidth, bitmapHeight) / 2f)
    }

    /**
     * Converts [borderWidthDp] into a pixel stroke in the source bitmap's coordinate space, using the
     * same dp-to-pixel ratio as [resolveCornerRadiusPx] so the border and the corner curve agree.
     *
     * Falls back to [PTConstants.PT_MEDIA_BORDER_WIDTH_DEFAULT] when the payload omits the key.
     * Clamped at both ends: a negative payload value must not produce a negative stroke, which would
     * both drop the border and push the draw rect outside the bitmap bounds.
     */
    internal fun resolveBorderWidthPx(
        bitmapWidth: Int,
        bitmapHeight: Int,
        borderWidthDp: Int?,
        mediaWidthDp: Float
    ): Float {
        val dp = (borderWidthDp ?: PTConstants.PT_MEDIA_BORDER_WIDTH_DEFAULT)
            .coerceIn(0, PTConstants.PT_MEDIA_BORDER_WIDTH_MAX)
        if (dp == 0 || mediaWidthDp <= 0f || bitmapWidth <= 0) return 0f
        val pixelsPerDp = bitmapWidth / mediaWidthDp
        return (dp * pixelsPerDp).coerceAtMost(minOf(bitmapWidth, bitmapHeight) * MAX_BORDER_RATIO)
    }
}
