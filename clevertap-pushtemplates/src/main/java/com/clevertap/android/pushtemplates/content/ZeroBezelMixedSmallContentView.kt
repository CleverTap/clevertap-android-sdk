package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class ZeroBezelMixedSmallContentView(context: Context, renderer: TemplateRenderer) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_zero_bezel, renderer) {

    init {
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type, renderer.pt_big_img_alt_text)
    }
}