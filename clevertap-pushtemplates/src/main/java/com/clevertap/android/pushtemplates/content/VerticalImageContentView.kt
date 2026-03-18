package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
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
        if (buttonData == null || buttonData.name.isNullOrEmpty()) return

        remoteView.setViewVisibility(R.id.vertical_img_btn_frame, View.VISIBLE)
        remoteView.setTextViewText(R.id.vertical_img_btn, buttonData.name)

        buttonData.textColor?.let { color ->
            Utils.getColourOrNull(color)?.let {
                remoteView.setTextColor(R.id.vertical_img_btn, it)
            }
        }

        val bitmap = when (buttonData.style) {
            ButtonStyle.GRADIENT -> {
                val color1 = buttonData.gradientColor1?.let { Utils.getColourOrNull(it) } ?: FALLBACK_BTN_COLOR
                val color2 = buttonData.gradientColor2?.let { Utils.getColourOrNull(it) } ?: FALLBACK_BTN_COLOR
                NotificationBitmapUtils.createGradientBitmap(color1, color2, buttonData.gradientDirection, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, BTN_CORNER_RADIUS)
            }
            ButtonStyle.SOLID -> {
                val bgColor = buttonData.buttonColor?.let { Utils.getColourOrNull(it) }
                val borderColor = buttonData.borderColor?.let { Utils.getColourOrNull(it) }
                NotificationBitmapUtils.createSolidBitmap(bgColor ?: FALLBACK_BTN_COLOR, borderColor, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, BTN_CORNER_RADIUS)
            }
        }
        remoteView.setImageViewBitmap(R.id.vertical_img_btn_bg, bitmap)

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
        const val FALLBACK_BTN_COLOR = 0xFFE91E63.toInt() // Pink
        private const val BTN_BITMAP_WIDTH = 100
        private const val BTN_BITMAP_HEIGHT = 30
        private const val BTN_CORNER_RADIUS = BTN_BITMAP_HEIGHT * 0.125f  // 12.5% of height
    }
}
