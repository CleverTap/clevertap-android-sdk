package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal open class ZeroBezelSmallContentView(context: Context, layoutId: Int, renderer: TemplateRenderer) :
    ContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_small)
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        setCustomContentViewSmallIcon()
    }
}