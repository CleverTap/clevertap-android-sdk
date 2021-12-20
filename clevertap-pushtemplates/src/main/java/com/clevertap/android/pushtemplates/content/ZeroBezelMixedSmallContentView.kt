package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class ZeroBezelMixedSmallContentView(context: Context, renderer: TemplateRenderer) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_zero_bezel, renderer) {

    init {
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewBigImage(renderer.pt_big_img)
    }

    private fun setCustomContentViewBigImage(pt_big_img: String?) {
        if (pt_big_img != null && pt_big_img.isNotEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.big_image, pt_big_img, remoteView)
            if (Utils.getFallback()) {
                remoteView.setViewVisibility(R.id.big_image, View.GONE)
            }
        } else {
            remoteView.setViewVisibility(R.id.big_image, View.GONE)
        }
    }
}