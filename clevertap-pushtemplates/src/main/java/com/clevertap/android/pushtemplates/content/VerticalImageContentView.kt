package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.VerticalImageButtonData

internal abstract class VerticalImageContentView(
    context: Context,
    renderer: TemplateRenderer,
    layoutId: Int,
    private val extras: Bundle
) : ActionButtonsContentView(context, renderer, layoutId) {

    protected fun setupButton(
        buttonData: VerticalImageButtonData?,
        notificationId: Int,
    ) {
        if (buttonData == null) {
            PTLog.debug("VerticalImageContentView: buttonData is null, skipping button setup")
            return
        }

        remoteView.setViewVisibility(R.id.vertical_img_btn_frame, View.VISIBLE)
        remoteView.setTextViewText(R.id.vertical_img_btn, buttonData.name)

        setCustomTextColour(buttonData.textColor, R.id.vertical_img_btn)

        val bitmap = when (buttonData.style) {
            ButtonStyle.GRADIENT_LINEAR -> {
                val color1 = buttonData.gradientColor1?.let { Utils.getColourOrNull(it) }
                val color2 = buttonData.gradientColor2?.let { Utils.getColourOrNull(it) }
                if (color1 != null && color2 != null) {
                    NotificationBitmapUtils.createLinearGradientBitmap(
                        color1, color2, buttonData.gradientDirection,
                        BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, BTN_CORNER_RADIUS
                    )
                } else null
            }
            ButtonStyle.GRADIENT_RADIAL -> {
                val color1 = buttonData.gradientColor1?.let { Utils.getColourOrNull(it) }
                val color2 = buttonData.gradientColor2?.let { Utils.getColourOrNull(it) }
                if (color1 != null && color2 != null) {
                    NotificationBitmapUtils.createRadialBitmap(
                        color1, color2, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, BTN_CORNER_RADIUS
                    )
                } else null
            }
            ButtonStyle.SOLID -> {
                val bgColor = buttonData.buttonColor?.let { Utils.getColourOrNull(it) }
                val borderColor = buttonData.borderColor?.let { Utils.getColourOrNull(it) }
                if (bgColor != null) {
                    NotificationBitmapUtils.createSolidBitmap(bgColor, borderColor, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, BTN_CORNER_RADIUS)
                } else null
            }
        }
        bitmap?.let { remoteView.setImageViewBitmap(R.id.vertical_img_btn_bg, it) }

        setupButtonDeepLink(extras, buttonData.deepLink, notificationId)
    }

    private fun setupButtonDeepLink(extras: Bundle, buttonDeepLink: String?, notificationId: Int) {
        val btnExtras = extras.clone() as Bundle
        val pendingIntent = PendingIntentFactory.getPendingIntent(
            context, notificationId, btnExtras, false, VERTICAL_IMAGE_CONTENT_PENDING_INTENT, buttonDeepLink
        ) ?: return
        remoteView.setOnClickPendingIntent(R.id.vertical_img_btn_frame, pendingIntent)
    }

    companion object {
        const val BTN_BITMAP_WIDTH = 100
        const val BTN_BITMAP_HEIGHT = 30
        private const val BTN_CORNER_RADIUS_RATIO = 0.125f
        const val BTN_CORNER_RADIUS = BTN_BITMAP_HEIGHT * BTN_CORNER_RADIUS_RATIO
    }
}
