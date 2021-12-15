package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.TemplateRenderer

open class ZeroBezelSmallContentView(context: Context, layoutId: Int, renderer: TemplateRenderer) :
    ContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewCollapsedBackgroundColour(renderer.pt_bg)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewSmallIcon()
        setCustomContentViewDotSep()
    }
}