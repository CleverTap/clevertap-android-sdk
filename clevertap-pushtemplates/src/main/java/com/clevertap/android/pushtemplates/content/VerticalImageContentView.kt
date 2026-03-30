package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import androidx.core.graphics.createBitmap
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.GradientDirection
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.VerticalImageButtonData
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

internal abstract class VerticalImageContentView(
    context: Context,
    renderer: TemplateRenderer,
    layoutId: Int,
    private val extras: Bundle
) : ActionButtonsContentView(context, renderer, layoutId) {

    protected fun setupButton(
        buttonData: VerticalImageButtonData?,
    ) {
        if (buttonData == null) {
            PTLog.debug("VerticalImageContentView: buttonData is null, skipping button setup")
            return
        }

        remoteView.setViewVisibility(R.id.vertical_img_btn_frame, View.VISIBLE)
        remoteView.setTextViewText(R.id.vertical_img_btn, buttonData.name)

        setCustomTextColour(buttonData.textColor, R.id.vertical_img_btn)

        val bitmap = when (buttonData.style) {
            ButtonStyle.GRADIENT -> {
                val color1 = buttonData.gradientColor1?.let { Utils.getColourOrNull(it) }
                val color2 = buttonData.gradientColor2?.let { Utils.getColourOrNull(it) }
                if (color1 != null && color2 != null) {
                    createGradientButtonBitmap(color1, color2, buttonData.gradientDirection)
                } else null
            }
            ButtonStyle.SOLID -> {
                val bgColor = buttonData.buttonColor?.let { Utils.getColourOrNull(it) }
                val borderColor = buttonData.borderColor?.let { Utils.getColourOrNull(it) }
                if (bgColor != null) createSolidButtonBitmap(bgColor, borderColor) else null
            }
        }
        bitmap?.let { remoteView.setImageViewBitmap(R.id.vertical_img_btn_bg, it) }

        buttonData.deepLink?.let { dl ->
            val btnExtras = extras.clone() as Bundle
            btnExtras.putString(Constants.DEEP_LINK_KEY, dl)
            remoteView.setOnClickPendingIntent(
                R.id.vertical_img_btn_frame,
                LaunchPendingIntentFactory.getLaunchPendingIntent(btnExtras, context)
            )
        }
    }

    companion object {
        private const val BTN_BITMAP_WIDTH = 100
        private const val BTN_BITMAP_HEIGHT = 30
        private const val BTN_CORNER_RADIUS_RATIO = 0.125f  // 12.5% of height
        private const val BTN_BORDER_STROKE_RATIO = 0.10f  // 10% of height
        private const val BTN_BORDER_INSET_RATIO = BTN_BORDER_STROKE_RATIO / 6f
        private const val BTN_CORNER_RADIUS = BTN_BITMAP_HEIGHT * BTN_CORNER_RADIUS_RATIO

        fun createSolidButtonBitmap(
            bgColor: Int,
            borderColor: Int?,
        ): Bitmap {
            val bitmap = createBitmap(BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT)
            val canvas = Canvas(bitmap)
            val rect = RectF(0f, 0f, BTN_BITMAP_WIDTH.toFloat(), BTN_BITMAP_HEIGHT.toFloat())

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
            canvas.drawRoundRect(rect, BTN_CORNER_RADIUS, BTN_CORNER_RADIUS, paint)

            if (borderColor != null) {
                val strokeWidth = BTN_BITMAP_HEIGHT * BTN_BORDER_STROKE_RATIO
                val inset = BTN_BITMAP_HEIGHT * BTN_BORDER_INSET_RATIO
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = borderColor
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                }
                val borderRect = RectF(inset, inset, BTN_BITMAP_WIDTH - inset, BTN_BITMAP_HEIGHT - inset)
                canvas.drawRoundRect(borderRect, BTN_CORNER_RADIUS, BTN_CORNER_RADIUS, borderPaint)
            }

            return bitmap
        }

        fun createGradientButtonBitmap(
            color1: Int,
            color2: Int,
            direction: GradientDirection,
        ): Bitmap {
            val bitmap = createBitmap(BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT)
            val canvas = Canvas(bitmap)
            val rect = RectF(0f, 0f, BTN_BITMAP_WIDTH.toFloat(), BTN_BITMAP_HEIGHT.toFloat())

            val shader = createShader(color1, color2, direction, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRoundRect(rect, BTN_CORNER_RADIUS, BTN_CORNER_RADIUS, paint)

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
                GradientDirection.RIGHT_LEFT   -> LinearGradient(w, 0f, 0f, 0f, color1, color2, Shader.TileMode.CLAMP)
                GradientDirection.TOP_BOTTOM   -> LinearGradient(0f, 0f, 0f, h, color1, color2, Shader.TileMode.CLAMP)
                GradientDirection.BOTTOM_TOP   -> LinearGradient(0f, h, 0f, 0f, color1, color2, Shader.TileMode.CLAMP)
                GradientDirection.DIAGONAL_TL_BR -> LinearGradient(0f, 0f, w, h, color1, color2, Shader.TileMode.CLAMP)
                GradientDirection.DIAGONAL_BL_TR -> LinearGradient(0f, h, w, 0f, color1, color2, Shader.TileMode.CLAMP)
                GradientDirection.RADIAL       -> RadialGradient(
                    w / 2f, h / 2f,
                    maxOf(w, h) / 2f,
                    color1, color2,
                    Shader.TileMode.CLAMP
                )
                GradientDirection.LEFT_RIGHT   -> LinearGradient(0f, 0f, w, 0f, color1, color2, Shader.TileMode.CLAMP)
            }
        }
    }
}
