package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class TimerBigContentView(context: Context, timer_end: Int?, renderer: TemplateRenderer) :
    TimerSmallContentView(context, timer_end, renderer, R.layout.timer) {

    init {
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewBigImage(renderer.pt_big_img)
    }

    private fun setCustomContentViewMessageSummary(pt_msg_summary: String?) {
        if (pt_msg_summary != null && pt_msg_summary.isNotEmpty()) {
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