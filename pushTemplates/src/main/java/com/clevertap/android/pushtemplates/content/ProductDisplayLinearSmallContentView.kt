package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import java.util.ArrayList

class ProductDisplayLinearSmallContentView(context: Context,
                                           renderer: TemplateRenderer, extras: Bundle
):
    SmallContentView(context, renderer, R.layout.product_display_linear_collapsed) {

    init {
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewCollapsedBackgroundColour(renderer.pt_bg)
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
        setImageList()

        remoteView.setOnClickPendingIntent(
            R.id.small_image1_collapsed,
            PendingIntentFactory.getPendingIntent(context,renderer.notificationId,extras,
                true,PRODUCT_DISPLAY_CONTENT_SMALL1_PENDING_INTENT,renderer)
        )

        if (renderer.deepLinkList!!.size >= 2){
            remoteView.setOnClickPendingIntent(
                R.id.small_image2_collapsed,
                PendingIntentFactory.getPendingIntent(context,renderer.notificationId,extras,
                    true,PRODUCT_DISPLAY_CONTENT_SMALL2_PENDING_INTENT,renderer)
            )
        }

        if (renderer.deepLinkList!!.size >= 3){
            remoteView.setOnClickPendingIntent(
                R.id.small_image3_collapsed,
                PendingIntentFactory.getPendingIntent(context,renderer.notificationId,extras,
                    true,PRODUCT_DISPLAY_CONTENT_SMALL3_PENDING_INTENT,renderer)
            )
        }

    }

    internal fun setImageList(){
        val imageCounter = 0

        val smallCollapsedImageLayoutIds = ArrayList<Int>()
        smallCollapsedImageLayoutIds.add(R.id.small_image1_collapsed)
        smallCollapsedImageLayoutIds.add(R.id.small_image2_collapsed)
        smallCollapsedImageLayoutIds.add(R.id.small_image3_collapsed)

        for (index in renderer.imageList!!.indices) {
            Utils.loadImageURLIntoRemoteView(
                smallCollapsedImageLayoutIds[imageCounter],
                renderer.imageList!![index],
                remoteView
            )
            remoteView.setViewVisibility(
                smallCollapsedImageLayoutIds[imageCounter],
                View.VISIBLE
            )
            if (!Utils.getFallback()) {
                remoteView.setViewVisibility(
                    smallCollapsedImageLayoutIds[imageCounter],
                    View.VISIBLE
                )
            }
        }
    }
}