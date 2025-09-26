package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData

internal open class ZeroBezelSmallContentView(
    context: Context,
    layoutId: Int,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData
) :
    ContentView(context, layoutId, renderer.templateMediaManager) {

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_small)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
    }
}