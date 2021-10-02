package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.*
import java.util.ArrayList

open class ProductDisplayLinearBigContentView(context: Context,
          layoutId: Int=R.layout.product_display_linear_expanded,renderer: TemplateRenderer,extras: Bundle):
    ContentView(context,layoutId ,renderer) {

    init {
        setCustomContentViewBasicKeys()
        if (renderer.bigTextList!!.isNotEmpty()) setCustomContentViewText(R.id.product_name,renderer.bigTextList!![0])
        if (renderer.priceList!!.isNotEmpty()) setCustomContentViewText(R.id.product_price, renderer.priceList!![0])
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewButtonLabel(R.id.product_action,renderer.pt_product_display_action)
        setCustomContentViewButtonColour(R.id.product_action,renderer.pt_product_display_action_clr)
        setCustomContentViewButtonText(R.id.product_action,renderer.pt_product_display_action_text_clr)
        setImageList()

        setCustomContentViewDotSep()
        setCustomContentViewSmallIcon()


        remoteView.setOnClickPendingIntent(R.id.small_image1, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false, PRODUCT_DISPLAY_DL1_PENDING_INTENT,renderer))

        if (renderer.deepLinkList!!.size >= 2) {
            remoteView.setOnClickPendingIntent(R.id.small_image2, PendingIntentFactory.getPendingIntent(context,
                renderer.notificationId, extras,false, PRODUCT_DISPLAY_DL2_PENDING_INTENT,renderer))
        }

        if (renderer.deepLinkList!!.size >= 3) {
            remoteView.setOnClickPendingIntent(R.id.small_image3, PendingIntentFactory.getPendingIntent(context,
                renderer.notificationId, extras,false, PRODUCT_DISPLAY_DL3_PENDING_INTENT,renderer))
        }

        remoteView.setOnClickPendingIntent(R.id.product_action, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false, PRODUCT_DISPLAY_BUY_NOW_PENDING_INTENT,renderer))
    }


    internal fun setImageList(){
        var imageCounter = 0
        var isFirstImageOk = false
        val smallImageLayoutIds = ArrayList<Int>()
        smallImageLayoutIds.add(R.id.small_image1)
        smallImageLayoutIds.add(R.id.small_image2)
        smallImageLayoutIds.add(R.id.small_image3)
        val tempImageList = ArrayList<String>()
        for (index in renderer.imageList!!.indices) {

            Utils.loadImageURLIntoRemoteView(
                smallImageLayoutIds[imageCounter], renderer.imageList!![index], remoteView
            )
            val tempRemoteView = RemoteViews(context.packageName, R.layout.image_view)
            Utils.loadImageURLIntoRemoteView(R.id.fimg, renderer.imageList!![index], tempRemoteView)
            if (!Utils.getFallback()) {
                if (!isFirstImageOk) {
                    isFirstImageOk = true
                }
                remoteView.setViewVisibility(
                    smallImageLayoutIds[imageCounter],
                    View.VISIBLE
                )
                remoteView.addView(R.id.carousel_image, tempRemoteView)
                imageCounter++
                tempImageList.add(renderer.imageList!![index])
            } else {
                renderer.deepLinkList!!.removeAt(index)
                renderer.bigTextList!!.removeAt(index)
                renderer.smallTextList!!.removeAt(index)
                renderer.priceList!!.removeAt(index)
            }
        }

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


    private fun setCustomContentViewButtonColour(resourceID: Int, pt_product_display_action_clr: String?) {
        if (pt_product_display_action_clr != null && pt_product_display_action_clr.isNotEmpty()) {
            remoteView.setInt(
                resourceID,
                "setBackgroundColor",
                Utils.getColour(
                    pt_product_display_action_clr,
                    PTConstants.PT_PRODUCT_DISPLAY_ACTION_CLR_DEFAULTS
                )
            )
        }
    }

    internal fun setCustomContentViewButtonText(resourceID: Int, pt_product_display_action_text_clr: String?) {
        if (pt_product_display_action_text_clr != null && pt_product_display_action_text_clr.isNotEmpty()) {
            remoteView.setTextColor(
                resourceID,
                Utils.getColour(
                    pt_product_display_action_text_clr,
                    PTConstants.PT_PRODUCT_DISPLAY_ACTION_TEXT_CLR_DEFAULT
                )
            )
        }
    }
}