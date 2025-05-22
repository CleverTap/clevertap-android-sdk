package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal class ZeroBezelMixedSmallContentView(context: Context, renderer: TemplateRenderer) :
    ZeroBezelSmallContentView(context, R.layout.cv_small_zero_bezel, renderer) {

    init {
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type)
    }

    private fun setCustomContentViewBigImage(pt_big_img: String?, scaleType: PTScaleType) {
        if (pt_big_img.isNotNullAndEmpty()) {
            if (Utils.getFallback()) {
                return
            }

            val imageViewId = when (scaleType) {
                PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
                PTScaleType.CENTER_CROP -> R.id.big_image
            }
            Utils.loadImageURLIntoRemoteView(imageViewId, pt_big_img, remoteView, context)
            remoteView.setViewVisibility(imageViewId, View.VISIBLE)
        }
    }
}