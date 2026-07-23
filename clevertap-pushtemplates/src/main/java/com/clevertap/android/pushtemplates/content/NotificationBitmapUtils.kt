package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
            val strokeWidth = (borderWidth ?: (height * BORDER_STROKE_RATIO))
                .coerceAtMost(minOf(width, height) / 2f)
            val outerRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val clampedOuterRadius = cornerRadius.coerceAtMost(minOf(width / 2f, height / 2f))
            val innerRect = RectF(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth)
            // Scale inner radius proportionally so corners stay visually consistent
            // regardless of how small cornerRadius or how large borderWidth is.
            val innerCornerRadius = if (height > 0)
                clampedOuterRadius * (innerRect.height() / height.toFloat())
            else 0f

            // Draw fill first on the transparent bitmap so any alpha in the fill paint
            // composites against the notification background, not the border colour.
            canvas.drawRoundRect(innerRect, innerCornerRadius, innerCornerRadius, paint)

            // Draw border as a ring (outer rounded rect minus inner hole) so the border
            // never bleeds behind the fill area.
            val borderPath = Path().apply {
                addRoundRect(outerRect, clampedOuterRadius, clampedOuterRadius, Path.Direction.CW)
                addRoundRect(innerRect, innerCornerRadius, innerCornerRadius, Path.Direction.CCW)
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = borderColor }
            canvas.drawPath(borderPath, borderPaint)
        } else {
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }

    private const val BORDER_STROKE_RATIO = 0.10f
}
