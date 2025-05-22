package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal class ZeroBezelBigContentView(context: Context, renderer: TemplateRenderer) :
    ActionButtonsContentView(context, R.layout.zero_bezel, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type)
        setCustomContentViewSmallIcon()
    }

    private fun setCustomContentViewMessageSummary(pt_msg_summary: String?) {
        if (pt_msg_summary.isNotNullAndEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary))
            }
        }
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