package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class AutoCarouselContentView(context: Context, renderer: TemplateRenderer):
    BigImageContentView(context,renderer,R.layout.auto_carousel) {

    init {
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewViewFlipperInterval(renderer.pt_flip_interval)
        setViewFlipper()
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

    private fun setCustomContentViewViewFlipperInterval(interval: Int) {
        remoteView.setInt(R.id.view_flipper, "setFlipInterval", interval)
    }

    private fun setViewFlipper(){
        var imageCounter = 0
        for (index in renderer.imageList!!.indices) {
            val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view)
            Utils.loadImageURLIntoRemoteView(R.id.fimg, renderer.imageList!![index], tempRemoteView)
            if (!Utils.getFallback()) {
                remoteView.addView(R.id.view_flipper, tempRemoteView)
                imageCounter++
            } else {
                PTLog.debug("Skipping Image in Auto Carousel.")
            }
        }
    }
}