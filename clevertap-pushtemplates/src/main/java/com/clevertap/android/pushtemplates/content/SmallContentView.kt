package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal open class SmallContentView(
    context: Context,
    renderer: TemplateRenderer, layoutId: Int = R.layout.content_view_small_single_line_msg
) : ContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_small)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        setCustomContentViewSmallIcon()
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
    }
}