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
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.Constants
import java.util.ArrayList

internal open class ProductDisplayLinearBigContentView(
    context: Context,
    renderer: TemplateRenderer, extras: Bundle, layoutId: Int = R.layout.product_display_linear_expanded
) :
    ContentView(context, layoutId, renderer) {

    protected var productName: String = renderer.bigTextList!![0]
    private var productPrice: String = renderer.priceList!![0]
    protected var productMessage: String = renderer.smallTextList!![0]
    private var productDL: String = renderer.deepLinkList!![0]

    init {
        var currentPosition = 0
        val extrasFrom = extras.getString(Constants.EXTRAS_FROM, "")
        if (extrasFrom == "PTReceiver") {
            currentPosition = extras.getInt(PTConstants.PT_CURRENT_POSITION, 0)
            productName = renderer.bigTextList!![currentPosition]
            productPrice = renderer.priceList!![currentPosition]
            productMessage = renderer.smallTextList!![currentPosition]
            productDL = renderer.deepLinkList!![currentPosition]
        }
        setCustomContentViewBasicKeys()
        if (renderer.bigTextList!!.isNotEmpty()) setCustomContentViewText(R.id.product_name, productName)
        if (renderer.priceList!!.isNotEmpty()) setCustomContentViewText(R.id.product_price, productPrice)

        setCustomContentViewButtonLabel(R.id.product_action, renderer.pt_product_display_action)

        setCustomBackgroundColour(renderer.pt_bg, R.id.content_view_big)
        setCustomBackgroundColour(renderer.pt_product_display_action_clr, R.id.product_action)
        setCustomTextColour(renderer.pt_product_display_action_text_clr, R.id.product_action)

        setImageList(extras, renderer.pt_scale_type)
        remoteView.setDisplayedChild(R.id.carousel_image, currentPosition)

        setCustomContentViewSmallIcon()


        remoteView.setOnClickPendingIntent(
            R.id.small_image1, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, PRODUCT_DISPLAY_DL1_PENDING_INTENT, renderer
            )
        )

        if (renderer.deepLinkList!!.size >= 2) {
            remoteView.setOnClickPendingIntent(
                R.id.small_image2, PendingIntentFactory.getPendingIntent(
                    context,
                    renderer.notificationId, extras, false, PRODUCT_DISPLAY_DL2_PENDING_INTENT, renderer
                )
            )
        }

        if (renderer.deepLinkList!!.size >= 3) {
            remoteView.setOnClickPendingIntent(
                R.id.small_image3, PendingIntentFactory.getPendingIntent(
                    context,
                    renderer.notificationId, extras, false, PRODUCT_DISPLAY_DL3_PENDING_INTENT, renderer
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

    internal fun setImageList(extras: Bundle, scaleType: PTScaleType) {
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

        renderer.imageList?.forEachIndexed { index, imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText

            Utils.loadImageURLIntoRemoteView(
                smallImageLayoutIds[imageCounter], imageUrl, remoteView, context, altText
            )

            val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view_flipper_dynamic)
            Utils.loadImageURLIntoRemoteView(imageViewId, imageUrl, tempRemoteView, context, altText)

            if (!Utils.getFallback()) {
                if (!isFirstImageOk) {
                    isFirstImageOk = true
                }
                tempRemoteView.setViewVisibility(imageViewId, View.VISIBLE)
                remoteView.setViewVisibility(smallImageLayoutIds[imageCounter], View.VISIBLE)
                remoteView.addView(R.id.carousel_image, tempRemoteView)
                imageCounter++
                tempImageList.add(imageUrl)
            } else {
                renderer.deepLinkList?.removeAt(index)
                renderer.bigTextList?.removeAt(index)
                renderer.smallTextList?.removeAt(index)
                renderer.priceList?.removeAt(index)
            }
        }


        extras.putStringArrayList(PTConstants.PT_IMAGE_LIST, tempImageList)
        extras.putStringArrayList(PTConstants.PT_DEEPLINK_LIST, renderer.deepLinkList)
        extras.putStringArrayList(PTConstants.PT_BIGTEXT_LIST, renderer.bigTextList)
        extras.putStringArrayList(PTConstants.PT_SMALLTEXT_LIST, renderer.smallTextList)
        extras.putStringArrayList(PTConstants.PT_PRICE_LIST, renderer.priceList)

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

    private fun setCustomContentViewButtonLabel(resourceID: Int, pt_product_display_action: String?) {
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