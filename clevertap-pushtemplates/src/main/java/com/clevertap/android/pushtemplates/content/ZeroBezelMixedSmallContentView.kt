package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class ZeroBezelMixedSmallContentView(context: Context, renderer: TemplateRenderer) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_zero_bezel, renderer) {

    init {
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewMedia(R.layout.image_view_dynamic_relative)
    }
}