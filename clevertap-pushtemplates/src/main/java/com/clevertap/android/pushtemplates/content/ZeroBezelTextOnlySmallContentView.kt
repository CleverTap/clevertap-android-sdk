package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

class ZeroBezelTextOnlySmallContentView(context: Context, renderer: TemplateRenderer) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_text_only, renderer) {

    init {
        remoteView.setViewVisibility(R.id.msg, View.GONE)
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
    }
}