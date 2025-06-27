package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class TimerBigContentView(context: Context, timer_end: Int?, renderer: TemplateRenderer) :
    TimerSmallContentView(context, timer_end, renderer, R.layout.timer) {

    init {
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_big)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type, renderer.pt_big_img_alt_text)
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
}