package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData

internal class ZeroBezelBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData
) :
    ActionButtonsContentView(context, renderer, data.actions, R.layout.zero_bezel) {

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomContentViewMessageSummary(data.baseContent.textData.messageSummary)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewMedia(
            R.layout.image_view_dynamic_relative,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames
        )
        setCustomContentViewSmallIcon(data.baseContent.iconData.smallIconBitmap, renderer.smallIcon)
    }
}