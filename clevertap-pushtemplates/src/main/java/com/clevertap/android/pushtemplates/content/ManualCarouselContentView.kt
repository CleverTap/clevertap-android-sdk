package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.R.id
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.content.PendingIntentFactory.getPendingIntent
import com.clevertap.android.sdk.Constants
import java.util.ArrayList

class ManualCarouselContentView(context: Context, renderer: TemplateRenderer, extras: Bundle) :
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

        if (extras.containsKey(PTConstants.PT_RIGHT_SWIPE)) {
            val rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE)
            val currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT)
            val nextPosition: Int
            val prevPosition: Int
            val newPosition: Int

            if (currPosition == tempImageList.size - 1) {
                nextPosition = 0
            } else {
                nextPosition = currPosition + 1
            }
            if (currPosition == 0) {
                prevPosition = tempImageList.size - 1
            } else {
                prevPosition = currPosition - 1
            }
            remoteView.setDisplayedChild(R.id.carousel_image, currPosition)
            remoteView.setDisplayedChild(R.id.carousel_image_right, nextPosition)
            remoteView.setDisplayedChild(
                R.id.carousel_image_left,
                prevPosition
            )

            if (rightSwipe) {
                newPosition = nextPosition
                remoteView.showNext(id.carousel_image)
                remoteView.showNext(id.carousel_image_right)
                remoteView.showNext(id.carousel_image_left)
            } else {
                newPosition = prevPosition
                remoteView.showPrevious(id.carousel_image)
                remoteView.showPrevious(id.carousel_image_right)
                remoteView.showPrevious(id.carousel_image_left)
            }
            var dl = ""
            val deepLinkList = renderer.deepLinkList
            if (deepLinkList != null && deepLinkList.size == tempImageList.size) {
                dl = deepLinkList.get(newPosition)
            } else if (deepLinkList != null && deepLinkList.size == 1) {
                dl = deepLinkList.get(0)
            } else if (deepLinkList != null && deepLinkList.size > newPosition) {
                dl = deepLinkList.get(newPosition)
            } else if (deepLinkList != null && deepLinkList.size < newPosition) {
                dl = deepLinkList.get(0)
            }

            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition)
            extras.remove(PTConstants.PT_RIGHT_SWIPE)
            extras.putString(Constants.DEEP_LINK_KEY, dl)
            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition)

            remoteView.setOnClickPendingIntent(
                id.rightArrowPos0, getPendingIntent(
                    context, renderer.notificationId, extras, false,
                    MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT, null
                )
            )

            remoteView.setOnClickPendingIntent(
                id.leftArrowPos0, getPendingIntent(
                    context, renderer.notificationId, extras, false,
                    MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT, null
                )
            )
        } else {
            remoteView.setDisplayedChild(R.id.carousel_image_right, 1)
            remoteView.setDisplayedChild(R.id.carousel_image, 0)
            remoteView.setDisplayedChild(
                R.id.carousel_image_left,
                tempImageList.size - 1
            )

            extras.putInt(
                PTConstants.PT_MANUAL_CAROUSEL_CURRENT,
                currentPosition
            )
            extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
            extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, renderer.deepLinkList)

            extras.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList!![0])
            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, 0)
            remoteView.setOnClickPendingIntent(
                R.id.rightArrowPos0,
                PendingIntentFactory.getPendingIntent(
                    context, renderer.notificationId, extras, false,
                    MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT, renderer
                )
            )

            remoteView.setOnClickPendingIntent(
                R.id.leftArrowPos0,
                PendingIntentFactory.getPendingIntent(
                    context, renderer.notificationId, extras, false,
                    MANUAL_CAROUSEL_LEFT_ARROW_PENDING_INTENT, renderer
                )
            )


            if (imageCounter < 2) {
                PTLog.debug("Need at least 2 images to display Manual Carousel, found - $imageCounter, not displaying the notification.")
            }
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