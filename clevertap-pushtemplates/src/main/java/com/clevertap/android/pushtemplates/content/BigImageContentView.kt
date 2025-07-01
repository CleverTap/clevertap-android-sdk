package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal open class BigImageContentView(
    context: Context, renderer: TemplateRenderer, layoutId: Int = R.layout.image_only_big
) : ActionButtonsContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_big)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewSmallIcon()
        setCustomContentViewBigImage(renderer.pt_big_img, renderer.pt_scale_type, renderer.pt_big_img_alt_text)
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
}