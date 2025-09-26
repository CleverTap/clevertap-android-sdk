package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal open class SmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: BaseContent,
    layoutId: Int = R.layout.content_view_small_single_line_msg,
) : ContentView(context, layoutId, renderer.templateMediaManager) {

    init {
        setCustomContentViewBasicKeys(data.textData.subtitle, data.colorData.metaColor)
        setCustomContentViewTitle(data.textData.title)
        setCustomContentViewMessage(data.textData.message)
        setCustomBackgroundColour(data.colorData.backgroundColor, R.id.content_view_small)
        setCustomTextColour(data.colorData.titleColor, R.id.title)
        setCustomTextColour(data.colorData.messageColor, R.id.msg)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewLargeIcon(data.iconData.largeIcon)
    }
}