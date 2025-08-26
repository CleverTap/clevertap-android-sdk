package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.ManualCarouselTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import java.util.ArrayList

internal class ManualCarouselContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ManualCarouselTemplateData,
    extras: Bundle,
) :
    ActionButtonsContentView(
        context,
        renderer,
        R.layout.manual_carousel,
    ) {

    init {
        val baseContent = data.carouselData.baseContent
        setCustomContentViewMessageSummary(baseContent.textData.messageSummary)
        setCustomContentViewBasicKeys(
            baseContent.textData.subtitle,
            baseContent.colorData.metaColor
        )
        setCustomContentViewTitle(baseContent.textData.title)
        setCustomContentViewMessage(baseContent.textData.subtitle)
        setCustomBackgroundColour(baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewSmallIcon(baseContent.iconData.smallIconBitmap, renderer.smallIcon)
        setCustomContentViewLargeIcon(baseContent.iconData.largeIcon)

        val scaleType = data.carouselData.scaleType

        remoteView.setViewVisibility(R.id.leftArrowPos0, View.VISIBLE)
        remoteView.setViewVisibility(R.id.rightArrowPos0, View.VISIBLE)
        var imageCounter = 0
        var isFirstImageOk = false
        var currentPosition = 0
        val tempImageList = ArrayList<String>()
        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }
        data.carouselData.imageList.forEachIndexed { index, imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText
            val tempRemoteView =
                RemoteViews(context.packageName, R.layout.image_view_dynamic_relative)

            val fallback = loadImageURLIntoRemoteView(
                imageViewId,
                imageUrl,
                tempRemoteView
            )

            if (!fallback) {
                if (!isFirstImageOk) {
                    currentPosition = index
                    isFirstImageOk = true
                }

                tempRemoteView.setViewVisibility(imageViewId, View.VISIBLE)
                val centerRemoteView = tempRemoteView.clone()

                // For filmstrip variant, only set the altText for the central image
                remoteView.addView(R.id.carousel_image_right, tempRemoteView)
                remoteView.addView(R.id.carousel_image_left, tempRemoteView)
                centerRemoteView.setContentDescription(imageViewId, altText)
                remoteView.addView(R.id.carousel_image, centerRemoteView)

                imageCounter++
                tempImageList.add(imageUrl!!)
            } else {
                val deepLinkList = data.carouselData.baseContent.deepLinkList
                if (imageCounter < deepLinkList.size) {
                    deepLinkList.removeAt(imageCounter)
                }
                PTLog.debug("Skipping Image in Manual Carousel.")
            }
        }

        if (data.carouselType == null || !data.carouselType.equals(
                PTConstants.PT_MANUAL_CAROUSEL_FILMSTRIP,
                ignoreCase = true
            )
        ) {
            remoteView.setViewVisibility(R.id.carousel_image_right, View.GONE)
            remoteView.setViewVisibility(R.id.carousel_image_left, View.GONE)
        }

        val deepLinkList = data.carouselData.baseContent.deepLinkList
        if (extras.containsKey(PTConstants.PT_RIGHT_SWIPE)) {
            val rightSwipe = extras.getBoolean(PTConstants.PT_RIGHT_SWIPE)
            val currPosition = extras.getInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT)
            val newPosition: Int

            val nextPosition: Int = if (currPosition == tempImageList.size - 1) {
                0
            } else {
                currPosition + 1
            }

            val prevPosition: Int = if (currPosition == 0) {
                tempImageList.size - 1
            } else {
                currPosition - 1
            }
            remoteView.setDisplayedChild(R.id.carousel_image, currPosition)
            remoteView.setDisplayedChild(R.id.carousel_image_right, nextPosition)
            remoteView.setDisplayedChild(
                R.id.carousel_image_left,
                prevPosition
            )

            if (rightSwipe) {
                newPosition = nextPosition
                remoteView.showNext(R.id.carousel_image)
                remoteView.showNext(R.id.carousel_image_right)
                remoteView.showNext(R.id.carousel_image_left)
            } else {
                newPosition = prevPosition
                remoteView.showPrevious(R.id.carousel_image)
                remoteView.showPrevious(R.id.carousel_image_right)
                remoteView.showPrevious(R.id.carousel_image_left)
            }
            var dl = ""
            if (deepLinkList.size == tempImageList.size) {
                dl = deepLinkList.get(newPosition)
            } else if (deepLinkList.size == 1) {
                dl = deepLinkList.get(0)
            } else if (deepLinkList.size > newPosition) {
                dl = deepLinkList.get(newPosition)
            } else if (deepLinkList.size < newPosition) {
                dl = deepLinkList.get(0)
            }

            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_CURRENT, newPosition)
            extras.remove(PTConstants.PT_RIGHT_SWIPE)
            extras.putString(Constants.DEEP_LINK_KEY, dl)
            extras.putInt(PTConstants.PT_MANUAL_CAROUSEL_FROM, currPosition)

            remoteView.setOnClickPendingIntent(
                R.id.rightArrowPos0, PendingIntentFactory.getPendingIntent(
                    context, renderer.notificationId, extras, false,
                    MANUAL_CAROUSEL_RIGHT_ARROW_PENDING_INTENT, null
                )
            )

            remoteView.setOnClickPendingIntent(
                R.id.leftArrowPos0, PendingIntentFactory.getPendingIntent(
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
            extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, deepLinkList)

            extras.putString(Constants.DEEP_LINK_KEY, deepLinkList[0])
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
}