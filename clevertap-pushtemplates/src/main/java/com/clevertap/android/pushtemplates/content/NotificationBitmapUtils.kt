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

    fun applyRoundedBorderToBitmap(
        source: Bitmap,
        cornerRadius: Float,
        borderColor: Int?,
        borderWidth: Float?
    ): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        if (cornerRadius <= 0f && borderColor == null) return source

        val minDimension = minOf(width, height)

        // coerceIn (not coerceAtMost) so a negative payload value can't disable the border or
        // push drawRect outside the bitmap bounds
        val strokeWidth = if (borderColor != null) {
            (borderWidth ?: (minDimension * BORDER_STROKE_RATIO))
                .coerceIn(0f, minDimension * MAX_BORDER_RATIO)
        } else 0f

        val half = strokeWidth / 2f
        val drawRect = RectF(half, half, width - half, height - half)
        // Cap at half the shortest side; beyond that the shape is already fully pill-shaped
        val safeRadius = cornerRadius.coerceIn(0f, minDimension / 2f)
        val adjustedRadius = (safeRadius - half).coerceAtLeast(0f)

        val output = createBitmap(width, height)
        val canvas = Canvas(output)

        // BitmapShader gives anti-aliased rounded corners without clipPath's jagged edges
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(drawRect, adjustedRadius, adjustedRadius, imagePaint)

        if (borderColor != null && strokeWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = borderColor
            }
            canvas.drawRoundRect(drawRect, adjustedRadius, adjustedRadius, borderPaint)
        }

        return output
    }

    private const val MAX_BORDER_RATIO = 0.25f
}
