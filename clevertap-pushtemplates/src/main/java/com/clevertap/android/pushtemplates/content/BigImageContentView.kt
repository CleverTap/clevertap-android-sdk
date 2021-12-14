package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

open class BigImageContentView(context: Context,renderer: TemplateRenderer,layoutId: Int=R.layout.image_only_big):
    ContentView(context, layoutId, renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewSmallIcon()
        setCustomContentViewDotSep()
        setCustomContentViewBigImage(renderer.pt_big_img)
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
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