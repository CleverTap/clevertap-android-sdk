package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTConstants.Companion.PT_ACTION_ID
import com.clevertap.android.pushtemplates.PTConstants.Companion.PT_NOTIF_ID
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.VerticalImageButtonData
import com.clevertap.android.sdk.Constants
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("UnnecessaryAbstractClass") // base of the small/big vertical-image views, never built directly
internal abstract class VerticalImageContentView(
    context: Context,
    renderer: TemplateRenderer,
    layoutId: Int,
    private val extras: Bundle
) : ActionButtonsContentView(context, renderer, layoutId) {

    protected fun setupButton(
        buttonData: VerticalImageButtonData?,
        notificationId: Int,
        isCollapsed: Boolean = false,
    ) {
        if (buttonData == null) {
            PTLog.debug("VerticalImageContentView: buttonData is null, skipping button setup")
            return
        }

        remoteView.setViewVisibility(R.id.vertical_img_btn_frame, View.VISIBLE)
        remoteView.setTextViewText(R.id.vertical_img_btn, buttonData.name)

        setCustomTextColour(buttonData.textColor, R.id.vertical_img_btn)

        val bitmap = buttonBackgroundBitmap(buttonData)
        bitmap?.let { remoteView.setImageViewBitmap(R.id.vertical_img_btn_bg, it) }

        setupButtonDeepLink(extras, buttonData.deepLink, notificationId, isCollapsed)
    }

    /**
     * The button background for [buttonData]'s style, or `null` when a color that style needs is
     * missing or invalid.
     */
    private fun buttonBackgroundBitmap(buttonData: VerticalImageButtonData): Bitmap? {
        val borderColor = colorOrNull(buttonData.borderColor)
        val borderWidth = buttonData.borderWidth
        val borderRadius = buttonData.borderRadius
        return when (buttonData.style) {
            ButtonStyle.GRADIENT_LINEAR -> {
                val color1 = colorOrNull(buttonData.gradientColor1)
                val color2 = colorOrNull(buttonData.gradientColor2)
                if (color1 != null && color2 != null) {
                    NotificationBitmapUtils.createLinearGradientBitmap(
                        color1, color2, buttonData.gradientDirection,
                        BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, borderRadius,
                        borderColor, borderWidth
                    )
                } else {
                    null
                }
            }
            ButtonStyle.GRADIENT_RADIAL -> {
                val color1 = colorOrNull(buttonData.gradientColor1)
                val color2 = colorOrNull(buttonData.gradientColor2)
                if (color1 != null && color2 != null) {
                    NotificationBitmapUtils.createRadialBitmap(
                        color1, color2, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, borderRadius,
                        borderColor, borderWidth
                    )
                } else {
                    null
                }
            }
            ButtonStyle.SOLID -> {
                val bgColor = colorOrNull(buttonData.buttonColor)
                bgColor?.let {
                    NotificationBitmapUtils.createSolidBitmap(
                        it, borderColor, BTN_BITMAP_WIDTH, BTN_BITMAP_HEIGHT, borderRadius,
                        borderWidth
                    )
                }
            }
        }
    }

    /** The parsed [colour], or `null` when it is missing or invalid. */
    private fun colorOrNull(colour: String?): Int? = colour?.let { Utils.getColourOrNull(it) }

    private fun setupButtonDeepLink(
        extras: Bundle,
        buttonDeepLink: String?,
        notificationId: Int,
        isCollapsed: Boolean
    ) {
        val btnExtras = extras.clone() as Bundle
        val actionId = if (isCollapsed) { PTConstants.PT_VT_C2A_COLLAPSED_KEY } else { PTConstants.PT_VT_C2A_KEY }

        btnExtras.putString(
            PT_ACTION_ID,
            actionId
        )

        btnExtras.putString(
            Constants.KEY_C2A,
            actionId
        )

        btnExtras.putInt(
            PT_NOTIF_ID,
            notificationId
        )

        val pendingIntent = PendingIntentFactory.getPendingIntent(
            context,
            notificationId,
            btnExtras,
            false,
            VERTICAL_IMAGE_BUTTON_PENDING_INTENT,
            buttonDeepLink
        ) ?: return
        remoteView.setOnClickPendingIntent(R.id.vertical_img_btn_frame, pendingIntent)
    }

    companion object {
        const val BTN_BITMAP_WIDTH = 100
        const val BTN_BITMAP_HEIGHT = 30
    }
}
