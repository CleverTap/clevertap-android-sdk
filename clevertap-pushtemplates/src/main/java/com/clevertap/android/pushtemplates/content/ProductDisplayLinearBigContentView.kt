package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.ProductTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import java.util.ArrayList

internal open class ProductDisplayLinearBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ProductTemplateData,
    extras: Bundle,
    layoutId: Int = R.layout.product_display_linear_expanded
) :
    ContentView(context, layoutId, renderer.templateMediaManager) {

    protected var productName: String = data.bigTextList[0]
    private var productPrice: String = data.priceList[0]
    protected var productMessage: String = data.smallTextList[0]
    private var productDL: String = data.baseContent.deepLinkList[0]

    init {
        var currentPosition = 0
        val extrasFrom = extras.getString(Constants.EXTRAS_FROM, "")
        if (extrasFrom == "PTReceiver") {
            currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION, 0)
            productName = data.bigTextList[currentPosition]
            productPrice = data.priceList[currentPosition]
            productMessage = data.smallTextList[currentPosition]
            productDL = data.baseContent.deepLinkList[currentPosition]
        }
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)

        if (data.bigTextList.isNotEmpty()) setCustomContentViewText(
            R.id.product_name,
            productName
        )
        if (data.priceList.isNotEmpty()) setCustomContentViewText(
            R.id.product_price,
            productPrice
        )

        setCustomContentViewButtonLabel(R.id.product_action, data.displayActionText)

        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomBackgroundColour(data.displayActionColor, R.id.product_action)
        setCustomTextColour(data.displayActionTextColor, R.id.product_action)

        setImageList(data, data.scaleType, extras)
        remoteView.setDisplayedChild(R.id.carousel_image, currentPosition)

        setCustomContentViewSmallIcon(data.baseContent.iconData.smallIconBitmap, renderer.smallIcon)

        remoteView.setOnClickPendingIntent(
            R.id.small_image1, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, PRODUCT_DISPLAY_DL1_PENDING_INTENT, data.baseContent.deepLinkList.getOrNull(0)
            )
        )

        if (data.baseContent.deepLinkList.size >= 2) {
            remoteView.setOnClickPendingIntent(
                R.id.small_image2, PendingIntentFactory.getPendingIntent(
                    context,
                    renderer.notificationId,
                    extras,
                    false,
                    PRODUCT_DISPLAY_DL2_PENDING_INTENT,
                    data.baseContent.deepLinkList.getOrNull(1)
                )
            )
        }

        if (data.baseContent.deepLinkList.size >= 3) {
            remoteView.setOnClickPendingIntent(
                R.id.small_image3, PendingIntentFactory.getPendingIntent(
                    context,
                    renderer.notificationId,
                    extras,
                    false,
                    PRODUCT_DISPLAY_DL3_PENDING_INTENT,
                    data.baseContent.deepLinkList.getOrNull(2)
                )
            )
        }

        val bundleBuyNow = extras.clone() as Bundle
        bundleBuyNow.putBoolean(PTConstants.PT_IMAGE_1, true)
        bundleBuyNow.putInt(PTConstants.PT_NOTIF_ID, renderer.notificationId)
        bundleBuyNow.putString(PTConstants.PT_BUY_NOW_DL, productDL)
        bundleBuyNow.putBoolean(PTConstants.PT_BUY_NOW, true)
        remoteView.setOnClickPendingIntent(
            R.id.product_action, PendingIntentFactory.getCtaLaunchPendingIntent(
                context,
                bundleBuyNow, productDL, renderer.notificationId
            )
        )
    }

    internal fun setImageList(data: ProductTemplateData, scaleType: PTScaleType, extras: Bundle) {
        var imageCounter = 0
        var isFirstImageOk = false
        val smallImageLayoutIds = ArrayList<Int>()
        smallImageLayoutIds.add(R.id.small_image1)
        smallImageLayoutIds.add(R.id.small_image2)
        smallImageLayoutIds.add(R.id.small_image3)
        val tempImageList = ArrayList<String>()
        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        data.imageList.forEachIndexed { index, imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText

            loadImageURLIntoRemoteView(
                smallImageLayoutIds[imageCounter], imageUrl, remoteView, altText
            )

            val tempRemoteView =
                RemoteViews(context.packageName, R.layout.image_view_dynamic_relative)
            val fallback =
                loadImageURLIntoRemoteView(imageViewId, imageUrl, tempRemoteView, altText)

            if (!fallback) {
                if (!isFirstImageOk) {
                    isFirstImageOk = true
                }
                tempRemoteView.setViewVisibility(imageViewId, View.VISIBLE)
                remoteView.setViewVisibility(smallImageLayoutIds[imageCounter], View.VISIBLE)
                remoteView.addView(R.id.carousel_image, tempRemoteView)
                imageCounter++
                // Safe to use !! because loadImageURLIntoRemoteView returns false only when imageUrl is valid (non-null, non-blank, starts with https)
                tempImageList.add(imageUrl!!)
            } else {
                data.baseContent.deepLinkList.removeAt(index)
                data.bigTextList.removeAt(index)
                data.smallTextList.removeAt(index)
                data.priceList.removeAt(index)
            }
        }


        extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
        extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, data.baseContent.deepLinkList)
        extras.putStringArrayList(PTConstants.PT_BIGTEXT_LIST, data.bigTextList)
        extras.putStringArrayList(PTConstants.PT_SMALLTEXT_LIST, data.smallTextList)
        extras.putStringArrayList(PTConstants.PT_PRICE_LIST, data.priceList)

        if (imageCounter <= 1) {
            PTLog.debug("2 or more images are not retrievable, not displaying the notification.")
        }
    }

    internal fun setCustomContentViewText(resourceId: Int, s: String) {
        if (s.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    resourceId,
                    Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(resourceId, Html.fromHtml(s))
            }
        }
    }

    private fun setCustomContentViewButtonLabel(
        resourceID: Int,
        pt_product_display_action: String?
    ) {
        if (pt_product_display_action != null && pt_product_display_action.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    resourceID,
                    Html.fromHtml(pt_product_display_action, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(resourceID, Html.fromHtml(pt_product_display_action))
            }
        }
    }
}