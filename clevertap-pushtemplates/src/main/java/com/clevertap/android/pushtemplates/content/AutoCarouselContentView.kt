package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

internal class AutoCarouselContentView(context: Context, renderer: TemplateRenderer) :
    BigImageContentView(context, renderer, R.layout.auto_carousel) {

    init {
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewViewFlipperInterval(renderer.pt_flip_interval)
        setViewFlipper(renderer.pt_scale_type)
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

    private fun setViewFlipper(scaleType: PTScaleType) {
        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        renderer.imageList?.forEach { imageUrl ->
            val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view_flipper_dynamic)

            Utils.loadImageURLIntoRemoteView(
                imageViewId,
                imageUrl,
                tempRemoteView,
                context
            )

            if (!Utils.getFallback()) {
                remoteView.addView(R.id.view_flipper, tempRemoteView)
                tempRemoteView.setViewVisibility(imageViewId, View.VISIBLE)
            } else {
                PTLog.debug("Skipping Image in Auto Carousel.")
            }
        }
    }
}