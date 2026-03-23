package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import com.clevertap.android.pushtemplates.GradientDirection

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
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }

        if (borderColor != null) {
            val strokeWidth = borderWidth ?: (height * BORDER_STROKE_RATIO)
            val inset = strokeWidth / 2f
            val bgRect = RectF(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, paint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)
        } else {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }

        return bitmap
    }

    fun createGradientBitmap(
        color1: Int,
        color2: Int,
        direction: GradientDirection,
        width: Int,
        height: Int,
        cornerRadius: Float,
        borderColor: Int? = null,
        borderWidth: Float? = null
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val shader = createShader(color1, color2, direction, width, height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }

        if (borderColor != null) {
            val strokeWidth = borderWidth ?: (height * BORDER_STROKE_RATIO)
            val inset = strokeWidth / 2f
            val bgRect = RectF(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, paint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)
        } else {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
        return bitmap
    }

    private fun createShader(
        color1: Int,
        color2: Int,
        direction: GradientDirection,
        width: Int,
        height: Int
    ): Shader {
        val w = width.toFloat()
        val h = height.toFloat()
        return when (direction) {
            GradientDirection.RIGHT_LEFT     -> LinearGradient(w, 0f, 0f, 0f, color1, color2, Shader.TileMode.CLAMP)
            GradientDirection.TOP_BOTTOM     -> LinearGradient(0f, 0f, 0f, h, color1, color2, Shader.TileMode.CLAMP)
            GradientDirection.BOTTOM_TOP     -> LinearGradient(0f, h, 0f, 0f, color1, color2, Shader.TileMode.CLAMP)
            GradientDirection.DIAGONAL_TL_BR -> LinearGradient(0f, 0f, w, h, color1, color2, Shader.TileMode.CLAMP)
            GradientDirection.DIAGONAL_BL_TR -> LinearGradient(0f, h, w, 0f, color1, color2, Shader.TileMode.CLAMP)
            GradientDirection.RADIAL         -> RadialGradient(
                w / 2f, h / 2f,
                maxOf(w, h) / 2f,
                color1, color2,
                Shader.TileMode.CLAMP
            )
            GradientDirection.LEFT_RIGHT     -> LinearGradient(0f, 0f, w, 0f, color1, color2, Shader.TileMode.CLAMP)
        }
    }

    private const val BORDER_STROKE_RATIO = 0.10f
}
