package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import com.clevertap.android.pushtemplates.ImageBorderData
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

    /** Chronometer/button backgrounds fall back to this when the payload omits a border width. */
    private const val BORDER_STROKE_RATIO = 0.10f

    /**
     * Half the shortest side is already a fully rounded image, so a larger radius has no further
     * visible effect. Clamping here means an over-large payload value renders as a full circle
     * rather than being rejected.
     */
    internal const val MAX_CORNER_RADIUS_PERCENT = 50f

    /** Payload ceiling for `pt_img_border_width`, as a percentage of the image's shortest side. */
    internal const val MAX_BORDER_WIDTH_PERCENT = 10f


    /**
     * Draws [source] into a new bitmap with rounded corners and, when [border] carries a visible
     * stroke, a border drawn in the same pass.
     *
     * Returns [source] untouched when there is nothing to draw, so a payload without styling keys
     * never costs an allocation.
     */
    fun applyRoundedBorderToBitmap(source: Bitmap, border: ImageBorderData): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0 || !border.isActive) return source

        val minDimension = minOf(width, height)
        val strokeWidth =
            if (border.hasBorder) resolveBorderWidthPx(minDimension, border.borderWidthPercent) else 0f
        val safeRadius = resolveCornerRadiusPx(minDimension, border.cornerRadiusPercent)

        PTLog.debug(
            "Image styling on ${width}x$height bitmap: corner radius " +
                    "${border.cornerRadiusPercent}% -> ${safeRadius}px, border width " +
                    "${border.borderWidthPercent}% -> ${strokeWidth}px"
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

        val strokeColor = border.borderColor
        if (strokeWidth > 0f && strokeColor != null) {
            // The border is filled as a ring rather than drawn as a centred stroke. A centred
            // stroke puts its outer edge half a stroke-width outside its own path, so its outer
            // corner can never be rounder than (pathRadius + strokeWidth / 2) — which squares off
            // the corners the clip just created whenever the radius is smaller than half the
            // width. Describing the ring explicitly keeps the outer edge on the clip's curve for
            // every radius/width pair, and produces the identical result to the stroke in the
            // cases where a stroke was already correct.
            val ring = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    safeRadius, safeRadius, Path.Direction.CW
                )
                // The width cap keeps the inset rect non-empty, so this never inverts.
                val innerRadius = (safeRadius - strokeWidth).coerceAtLeast(0f)
                addRoundRect(
                    RectF(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth),
                    innerRadius, innerRadius, Path.Direction.CW
                )
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = strokeColor }
            canvas.drawPath(ring, borderPaint)
        }

        return output
    }

    /**
     * Resolves [cornerRadiusPercent] against the image's shortest side, clamped to
     * [MAX_CORNER_RADIUS_PERCENT].
     *
     * Kept separate from the drawing code so the conversion can be asserted directly; Canvas
     * operations are no-ops under Robolectric's legacy graphics mode, so a test that only inspects
     * the output bitmap cannot tell a percentage from a raw pixel count.
     */
    internal fun resolveCornerRadiusPx(minDimension: Int, cornerRadiusPercent: Float): Float =
        minDimension * cornerRadiusPercent.coerceIn(0f, MAX_CORNER_RADIUS_PERCENT) / 100f

    /**
     * Resolves [borderWidthPercent] against the image's shortest side, clamped to
     * [MAX_BORDER_WIDTH_PERCENT].
     *
     * Clamped at both ends: a negative payload value must not produce a negative stroke, which
     * would both drop the border and push the draw rect outside the bitmap bounds.
     */
    internal fun resolveBorderWidthPx(minDimension: Int, borderWidthPercent: Float): Float =
        minDimension * borderWidthPercent.coerceIn(0f, MAX_BORDER_WIDTH_PERCENT) / 100f
}
