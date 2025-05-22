package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.view.View
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal open class BigImageContentView(
    context: Context, renderer: TemplateRenderer, layoutId: Int = R.layout.image_only_big
) : ActionButtonsContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewSmallIcon()
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type)
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
    }

    private fun setCustomContentViewMessageSummary(pt_msg_summary: String?) {
        if (pt_msg_summary.isNotNullAndEmpty()) {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.msg, Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
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