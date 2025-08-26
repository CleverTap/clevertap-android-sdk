package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData

internal class ZeroBezelMixedSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData
) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_zero_bezel, renderer, data) {

    init {
        setCustomContentViewMessage(data.baseContent.textData.message)

        setCustomContentViewMedia(
            R.layout.image_view_dynamic_relative,
            data.collapsedMediaData.gif.url,
            data.collapsedMediaData.bigImage.url,
            data.collapsedMediaData.scaleType,
            data.collapsedMediaData.bigImage.altText,
            data.collapsedMediaData.gif.numberOfFrames
        )
    }
}