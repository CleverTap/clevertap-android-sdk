package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.VerticalImageTemplateData

internal class VerticalImageSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: VerticalImageTemplateData,
    extras: Bundle
) : VerticalImageContentView(context, renderer, R.layout.vertical_image_small, extras) {

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_small)

        val mediaData = data.collapsedMediaData
        if (mediaData != null) {
            setCustomContentViewMedia(
                R.layout.image_view_dynamic_relative,
                mediaData.gif.url,
                mediaData.bigImage.url,
                mediaData.scaleType,
                mediaData.bigImage.altText,
                mediaData.gif.numberOfFrames
            )
        } else {
            remoteView.setViewVisibility(R.id.big_media_configurable, View.GONE)
        }

        setupButton(data.collapsedButtonData)
    }

}
