package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.R.drawable
import com.clevertap.android.pushtemplates.R.id
import com.clevertap.android.pushtemplates.RatingTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.Constants
import java.util.*

internal class RatingContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: RatingTemplateData,
    extras: Bundle,
) :
    ContentView(context, R.layout.rating, renderer.templateMediaManager) {

    init {
        val baseContent = data.baseContent
        setCustomContentViewBasicKeys(
            baseContent.textData.subtitle,
            baseContent.colorData.metaColor
        )
        setCustomContentViewTitle(baseContent.textData.title)
        setCustomContentViewMessage(baseContent.textData.message)
        setCustomBackgroundColour(baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewMessageSummary(baseContent.textData.messageSummary)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewMedia(
            R.layout.image_view_dynamic_linear,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames,
            data.mediaData.imageBorderData
        )
        setCustomContentViewLargeIcon(baseContent.iconData.largeIcon)

        //Set rating stars
        val iconViewIds = listOf(R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5)
        val hasCustomIcons = data.icons.any { it.url != null }

        for (i in 0 until 5) {
            val viewId = iconViewIds[i]
            if (i >= data.iconCount) {
                remoteView.setViewVisibility(viewId, View.GONE)
            } else if (hasCustomIcons) {
                val colored = data.icons[i].url?.let { templateMediaManager.getImageBitmap(it) }
                if (colored != null) {
                    val grey = Utils.toGreyscale(colored)
                    if (grey != null) remoteView.setImageViewBitmap(viewId, grey)
                    else remoteView.setImageViewResource(viewId, R.drawable.pt_star_outline)
                }
                else remoteView.setImageViewResource(viewId, R.drawable.pt_star_outline)
            } else {
                remoteView.setImageViewResource(viewId, R.drawable.pt_star_outline)
            }
        }

        // Request Codes for all stars are passed as an extra to cancel all pending intents when any of the star is clicked
        val rng = Random()
        extras.putIntArray(PTConstants.KEY_REQUEST_CODES, IntArray(5) { rng.nextInt() })

        remoteView.setOnClickPendingIntent(
            R.id.star1, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK1_PENDING_INTENT, config = renderer.config
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star2, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK2_PENDING_INTENT, config = renderer.config
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star3, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK3_PENDING_INTENT, config = renderer.config
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star4, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK4_PENDING_INTENT, config = renderer.config
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star5, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK5_PENDING_INTENT, config = renderer.config
            )
        )

        remoteView.setViewVisibility(R.id.rating_confirm_frame, View.GONE)

        // Apply confirm button text
        val btnText = extras.getString(PTConstants.PT_RATING_CONFIRM_TEXT)
            ?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.confirm_btn)
        remoteView.setTextViewText(R.id.tVRatingConfirmation, btnText)

        // Apply confirm button text color
        setCustomTextColour(extras.getString(PTConstants.PT_BTN_TEXT_CLR), R.id.tVRatingConfirmation)

        // Apply confirm button background bitmap (color/gradient/border/radius)
        applyConfirmButtonBackground(extras)

        // Apply horizontal margin to center the button (opt-in via pt_rating_btn_margin_h)
        val marginH = extras.getString(PTConstants.PT_RATING_BTN_MARGIN_H)?.toIntOrNull()
        if (marginH != null && marginH > 0) {
            val px = (marginH * context.resources.displayMetrics.density).toInt()
            remoteView.setViewPadding(R.id.rating_confirm_frame, px, 0, px, 0)
        }

        val extrasFrom = extras.getString(Constants.EXTRAS_FROM, "")
        if (extrasFrom == "PTReceiver" && !hasCustomIcons) {
            if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_outline)
            }
            if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star2, drawable.pt_star_outline)
            }
            if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star3, drawable.pt_star_outline)
            }
            if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star4, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star4, drawable.pt_star_outline)
            }
            if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star4, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star5, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star5, drawable.pt_star_outline)
            }
        }
    }

    private fun applyConfirmButtonBackground(extras: Bundle) {
        val borderColor = extras.getString(PTConstants.PT_BTN_BORDER_CLR)?.let { Utils.getColourOrNull(it) }
        val borderRadius = extras.getString(PTConstants.PT_BTN_BORDER_RADIUS)?.toFloatOrNull()
            ?: PTConstants.PT_BTN_BORDER_RADIUS_DEFAULT
        val borderWidth = extras.getString(PTConstants.PT_BTN_BORDER_WIDTH)?.toFloatOrNull()
        val style = ButtonStyle.fromString(extras.getString(PTConstants.PT_BTN_STYLE))

        val bitmap: Bitmap? = when (style) {
            ButtonStyle.GRADIENT_LINEAR -> {
                val c1 = extras.getString(PTConstants.PT_BTN_GRAD_CLR1)?.let { Utils.getColourOrNull(it) }
                val c2 = extras.getString(PTConstants.PT_BTN_GRAD_CLR2)?.let { Utils.getColourOrNull(it) }
                val dir = extras.getString(PTConstants.PT_BTN_GRAD_DIR)?.toDoubleOrNull()
                    ?: PTConstants.PT_BTN_GRAD_DIR_DEFAULT
                if (c1 != null && c2 != null)
                    NotificationBitmapUtils.createLinearGradientBitmap(c1, c2, dir, 200, 50, borderRadius, borderColor, borderWidth)
                else null
            }
            ButtonStyle.GRADIENT_RADIAL -> {
                val c1 = extras.getString(PTConstants.PT_BTN_GRAD_CLR1)?.let { Utils.getColourOrNull(it) }
                val c2 = extras.getString(PTConstants.PT_BTN_GRAD_CLR2)?.let { Utils.getColourOrNull(it) }
                if (c1 != null && c2 != null)
                    NotificationBitmapUtils.createRadialBitmap(c1, c2, 200, 50, borderRadius, borderColor, borderWidth)
                else null
            }
            ButtonStyle.SOLID -> {
                val bgColor = extras.getString(PTConstants.PT_BTN_CLR)?.let { Utils.getColourOrNull(it) }
                if (bgColor != null)
                    NotificationBitmapUtils.createSolidBitmap(bgColor, borderColor, 200, 50, borderRadius, borderWidth)
                else null
            }
        }
        if (bitmap != null) {
            remoteView.setImageViewBitmap(R.id.rating_confirm_bg, bitmap)
            remoteView.setInt(R.id.rating_confirm_btn, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
        }
    }
}