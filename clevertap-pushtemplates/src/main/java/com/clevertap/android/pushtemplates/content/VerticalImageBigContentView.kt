package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.VerticalImageTemplateData
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal class VerticalImageBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: VerticalImageTemplateData,
    extras: Bundle
) : VerticalImageContentView(context, renderer, R.layout.vertical_image_big, extras) {

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomContentViewMessageSummary(data.baseContent.textData.messageSummary)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)

        setCustomContentViewMedia(
            R.layout.image_view_dynamic_linear,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames
        )

        setAdditionalText(data.text1, R.id.vertical_img_text1, data.text1Color)
        setAdditionalText(data.text2, R.id.vertical_img_text2, data.text2Color)

        setupButton(data.buttonData, renderer.notificationId)
    }

    private fun setAdditionalText(text: String?, viewId: Int, color: String?) {
        if (text.isNotNullAndEmpty()) {
            remoteView.setViewVisibility(viewId, View.VISIBLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(viewId, Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY))
            } else {
                remoteView.setTextViewText(viewId, Html.fromHtml(text))
            }
            setCustomTextColour(color, viewId)
        }
    }
}
