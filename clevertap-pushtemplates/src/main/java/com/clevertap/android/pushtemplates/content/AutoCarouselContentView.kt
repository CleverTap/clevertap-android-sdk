package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.AutoCarouselTemplateData
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class AutoCarouselContentView(
    context: Context,
    renderer: TemplateRenderer,
    internal val data: AutoCarouselTemplateData
) :
    ActionButtonsContentView(context, renderer, R.layout.auto_carousel) {

    init {
        val baseContent = data.carouselData.baseContent

        setCustomContentViewMessageSummary(baseContent.textData.messageSummary)
        setCustomContentViewBasicKeys(
            baseContent.textData.subtitle,
            baseContent.colorData.metaColor
        )
        setCustomContentViewTitle(baseContent.textData.title)
        setCustomContentViewMessage(baseContent.textData.message)
        setCustomBackgroundColour(baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewLargeIcon(baseContent.iconData.largeIcon)
        setCustomContentViewViewFlipperInterval(data.flipInterval)
        setViewFlipper()
    }

    private fun setCustomContentViewViewFlipperInterval(interval: Int) {
        remoteView.setInt(R.id.view_flipper, "setFlipInterval", interval)
    }

    private fun setViewFlipper() {
        val imageViewId = when (data.carouselData.scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        data.carouselData.imageList.forEach { imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText
            val tempRemoteView =
                RemoteViews(context.packageName, R.layout.image_view_dynamic_relative)

            val fallback = loadImageURLIntoRemoteView(
                imageViewId,
                imageUrl,
                tempRemoteView,
                altText
            )

            if (!fallback) {
                tempRemoteView.setViewVisibility(imageViewId, View.VISIBLE)
                remoteView.addView(R.id.view_flipper, tempRemoteView)
            } else {
                PTLog.debug("Skipping Image in Auto Carousel.")
            }
        }
    }
}