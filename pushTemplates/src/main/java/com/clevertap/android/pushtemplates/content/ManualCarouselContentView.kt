package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.*
import java.util.ArrayList

class ManualCarouselContentView(context: Context, renderer: TemplateRenderer,extras: Bundle):
    BigImageContentView(context, renderer, R.layout.manual_carousel) {


    init {
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)

        remoteView.setViewVisibility(R.id.leftArrowPos0, View.VISIBLE)
        remoteView.setViewVisibility(R.id.rightArrowPos0, View.VISIBLE)
        var imageCounter = 0
        var isFirstImageOk = false
        val dl = renderer.deepLinkList!![0]
        var currentPosition = 0
        val tempImageList = ArrayList<String>()
        for (index in renderer.imageList!!.indices) {
            val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view_rounded)
            Utils.loadImageURLIntoRemoteView(
                R.id.flipper_img,
                renderer.imageList!![index],
                tempRemoteView,
                context
            )
            if (!Utils.getFallback()) {
                if (!isFirstImageOk) {
                    currentPosition = index
                    isFirstImageOk = true
                }
                remoteView.addView(R.id.carousel_image, tempRemoteView)
                remoteView.addView(R.id.carousel_image_right, tempRemoteView)
                remoteView.addView(R.id.carousel_image_left, tempRemoteView)
                imageCounter++
                tempImageList.add(renderer.imageList!![index])
            } else {
                if (renderer.deepLinkList != null && renderer.deepLinkList!!.size == renderer.imageList!!.size) {
                    renderer.deepLinkList!!.removeAt(index)
                }
                PTLog.debug("Skipping Image in Manual Carousel.")
            }
        }
        if (renderer.pt_manual_carousel_type == null || !renderer.pt_manual_carousel_type.equals(
                PTConstants.PT_MANUAL_CAROUSEL_FILMSTRIP,
                ignoreCase = true
            )
        ) {
            remoteView.setViewVisibility(R.id.carousel_image_right, View.GONE)
            remoteView.setViewVisibility(R.id.carousel_image_left, View.GONE)
        }
        remoteView.setDisplayedChild(R.id.carousel_image_right, 1)
        remoteView.setDisplayedChild(
            R.id.carousel_image_left,
            tempImageList.size - 1
        )

        extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, currentPosition)//TODO Check extras in ManualCarousel style being updated
        extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
        extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, renderer.deepLinkList)

        remoteView.setOnClickPendingIntent(
            R.id.rightArrowPos0,
            PendingIntentFactory.getPendingIntent(context,renderer.notificationId, extras,false,
                MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT,renderer)
        )

        remoteView.setOnClickPendingIntent(
            R.id.leftArrowPos0,
            PendingIntentFactory.getPendingIntent(context,renderer.notificationId, extras,false,
                MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT,renderer)
        )


        if (imageCounter < 2) {
            PTLog.debug("Need at least 2 images to display Manual Carousel, found - $imageCounter, not displaying the notification.")
        }
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