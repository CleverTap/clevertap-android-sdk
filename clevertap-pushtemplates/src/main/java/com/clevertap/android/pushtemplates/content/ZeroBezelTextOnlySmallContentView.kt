package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData

internal class ZeroBezelTextOnlySmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData
) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_text_only, renderer, data) {

    init {
        remoteView.setViewVisibility(R.id.msg, View.GONE)
        setCustomContentViewLargeIcon(data.baseContent.iconData.largeIcon)
    }
}