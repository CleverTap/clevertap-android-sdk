package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal class ZeroBezelBigContentView(context: Context, renderer: TemplateRenderer) :
    ActionButtonsContentView(context, R.layout.zero_bezel, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_big)
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type, renderer.pt_big_img_alt_text)
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
}