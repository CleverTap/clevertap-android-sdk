package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData

internal open class ZeroBezelSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData,
    layoutId: Int = R.layout.cv_small_zero_bezel
) :
    SmallContentView(context, renderer, data.baseContent, layoutId) {

    init {
        val isMediaLoaded = setCustomContentViewMedia(
            R.layout.image_view_dynamic_relative,
            data.collapsedMediaData.gif.url,
            data.collapsedMediaData.bigImage.url,
            data.collapsedMediaData.scaleType,
            data.collapsedMediaData.bigImage.altText,
            data.collapsedMediaData.gif.numberOfFrames
        )
        if (!isMediaLoaded) {
            PTLog.debug("Download failed for all media in ZeroBezel Collapsed Notification. Not showing the image")
            remoteView.setViewVisibility(R.id.zero_bezel_scrim, View.GONE)
        }
    }
}