package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.BasicTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal open class BigImageContentView(
    context: Context, renderer: TemplateRenderer, data: BasicTemplateData,
    layoutId: Int = R.layout.image_only_big,
) : ActionButtonsContentView(context, renderer, layoutId) {

    init {
        setCustomContentViewBasicKeys(
            data.baseContent.textData.subtitle,
            data.baseContent.colorData.metaColor
        )
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewMessageSummary(data.baseContent.textData.messageSummary)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewMedia(
            R.layout.image_view_dynamic_linear,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames
        )
        setCustomContentViewLargeIcon(data.baseContent.iconData.largeIcon)
    }
}