package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
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

    /** Chronometer/button backgrounds fall back to this when the payload omits a border width. */
    private const val BORDER_STROKE_RATIO = 0.10f

    internal const val MAX_CORNER_RADIUS_PERCENT = 50f

    internal const val MAX_BORDER_WIDTH_VALUE = 100f

    internal const val BORDER_WIDTH_DIVISOR = 1000f

    private const val PERCENT_DIVISOR = 100f

    /** Smallest border width in px. */
    internal const val MIN_BORDER_WIDTH_PX = 1f

    /** Styled bitmaps may be this much taller than the style reference. */
    internal const val STYLED_IMAGE_HEIGHT_HEADROOM = 1.5f

    /** Height cap for styled bitmaps. */
    internal fun resolveStyledImageMaxHeightPx(referencePx: Int): Int =
        (referencePx * STYLED_IMAGE_HEIGHT_HEADROOM).toInt()

    /**
     * [source] scaled down so neither side exceeds [maxSidePx], or [source] itself when it already
     * fits. Aspect ratio is preserved.
     *
     * A notification's bitmaps are parcelled to the system at whatever resolution they were decoded
     * at — the size of the view they land in changes nothing, because an ImageView scales at draw
     * time and never shrinks what is stored. A 1200x1200 icon costs 5.76MB whether it is shown at
     * 1200dp or at 20dp. The platform applies its own cap in `Notification.Builder.build()`, but that
     * cap is sized for a full-width custom notification image, so an icon drawn at 20dp still arrives
     * an order of magnitude larger than it needs to be. Past roughly 5MB of bitmaps the system drops
     * the custom layout altogether and the template silently becomes a plain notification.
     *
     * Callers pass the size the bitmap is actually drawn at, so the cost is bounded by the layout
     * rather than by whatever the campaign happened to upload.
     *
     * @return a new bitmap the caller owns, or [source] unchanged — compare by identity before
     *         recycling, since recycling a cached source would corrupt every later render.
     */
    fun scaleDownTo(source: Bitmap, maxSidePx: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (maxSidePx <= 0 || width <= 0 || height <= 0) return source
        if (width <= maxSidePx && height <= maxSidePx) return source

        // The longest side lands exactly on the budget and the other is derived from it, so the
        // result is predictable rather than a pixel short from float rounding.
        val landscape = width >= height
        val targetWidth: Int
        val targetHeight: Int
        if (landscape) {
            targetWidth = maxSidePx
            targetHeight = Math.round(height * (maxSidePx.toFloat() / width)).coerceAtLeast(1)
        } else {
            targetHeight = maxSidePx
            targetWidth = Math.round(width * (maxSidePx.toFloat() / height)).coerceAtLeast(1)
        }

        return try {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true).also {
                PTLog.verbose(
                    "Scaled notification bitmap ${width}x$height -> ${targetWidth}x$targetHeight " +
                            "(${source.allocationByteCount} -> ${it.allocationByteCount} bytes)"
                )
            }
        } catch (t: Throwable) {
            PTLog.debug("Could not scale a notification bitmap down, using it as it is: $t")
            source
        }
    }

    /** Returns a copy of [source] with the corner radius and border applied. */
    fun applyRoundedBorderToBitmap(
        source: Bitmap,
        border: ImageBorderData,
        maxHeightPx: Int = 0
    ): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        if (sourceWidth <= 0 || sourceHeight <= 0 || !border.isActive) {
            return source
        }

        val scale = if (maxHeightPx in 1 until sourceHeight) {
            maxHeightPx / sourceHeight.toFloat()
        } else {
            1f
        }
        val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val height = (sourceHeight * scale).toInt().coerceAtLeast(1)

        logStyledImageScale(source, maxHeightPx, scale, width, height)

        // Keep the stroke and radius within half the shorter side for portrait images.
        val halfShortSide = minOf(width, height) / 2f
        val strokeWidth = if (border.hasBorder) {
            resolveBorderWidthPx(height, border.borderWidthValue).coerceAtMost(halfShortSide)
        } else {
            0f
        }
        val safeRadius = resolveCornerRadiusPx(height, border.cornerRadiusPercent)
            .coerceAtMost(halfShortSide)

        PTLog.debug(
            "Image styling on ${width}x$height bitmap: corner radius " +
                    "${border.cornerRadiusPercent}% -> ${safeRadius}px, border width " +
                    "${border.borderWidthValue} -> ${strokeWidth}px"
        )

        val output = createBitmap(width, height)
        val canvas = Canvas(output)

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                if (scale != 1f) {
                    setLocalMatrix(Matrix().apply { setScale(scale, scale) })
                }
            }
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            safeRadius, safeRadius, imagePaint
        )

        val strokeColor = border.borderColor
        if (strokeWidth > 0f && strokeColor != null) {
            // The stroke is centred on the rect, so inset by half its width to keep it inside.
            val halfStroke = strokeWidth / 2f
            val borderRadius = (safeRadius - halfStroke).coerceAtLeast(0f)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = strokeColor
            }
            canvas.drawRoundRect(
                RectF(halfStroke, halfStroke, width - halfStroke, height - halfStroke),
                borderRadius, borderRadius, borderPaint
            )
        }

        return output
    }

    private fun logStyledImageScale(source: Bitmap, maxHeightPx: Int, scale: Float, width: Int, height: Int) {
        if (scale != 1f) {
            PTLog.debug(
                "Styled image is taller than the ${maxHeightPx}px cap, drawing the copy at " +
                        "${width}x$height instead of ${source.width}x${source.height} (scale $scale)"
            )
        } else if (maxHeightPx > 0) {
            PTLog.verbose(
                "Styled image ${source.width}x${source.height} is within the ${maxHeightPx}px cap, " +
                        "drawing the copy at full size"
            )
        }
    }

    internal fun resolveCornerRadiusPx(reference: Int, cornerRadiusPercent: Float): Float =
        reference * cornerRadiusPercent.coerceIn(0f, MAX_CORNER_RADIUS_PERCENT) / PERCENT_DIVISOR

    /** Border width in px, at least [MIN_BORDER_WIDTH_PX] when a border is requested. */
    internal fun resolveBorderWidthPx(reference: Int, borderWidthValue: Float): Float {
        if (borderWidthValue <= 0f) {
            return 0f
        }
        val px = reference * borderWidthValue.coerceAtMost(MAX_BORDER_WIDTH_VALUE) / BORDER_WIDTH_DIVISOR
        return px.coerceAtLeast(MIN_BORDER_WIDTH_PX)
    }
}
