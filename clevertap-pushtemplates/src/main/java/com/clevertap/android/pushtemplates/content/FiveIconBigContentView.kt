package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

internal class FiveIconBigContentView constructor(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : ContentView(context, R.layout.five_cta_expanded, renderer.templateMediaManager) {

    private var imageCounter: Int = 0

    init {
        var title = data.title
        if (data.title == null || data.title.isEmpty()) {
            title = Utils.getApplicationName(context)
        }
        setCustomBackgroundColour(data.backgroundColor, R.id.content_view_big)
        val ctaIds = listOf(R.id.cta1, R.id.cta2, R.id.cta3, R.id.cta4, R.id.cta5)
        data.imageList.forEachIndexed { index, imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText
            if (index >= ctaIds.size) return@forEachIndexed

            val viewId = ctaIds[index]
            remoteView.setViewVisibility(viewId, View.VISIBLE)

            val fallback = loadImageURLIntoRemoteView(
                viewId,
                imageUrl,
                remoteView,
                altText
            )

            if (fallback) {
                remoteView.setViewVisibility(viewId, View.GONE)
                imageCounter++
            }
        }

        extras.putInt(PTConstants.PT_NOTIF_ID, renderer.notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)

        val deepLinkList = data.deepLinkList

        val bundleCTA1 = extras.clone() as Bundle
        bundleCTA1.putBoolean("cta1", true)
        bundleCTA1.putString(Constants.DEEP_LINK_KEY, deepLinkList.get(0))
        bundleCTA1.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 1 + "_" + deepLinkList.get(0))
        remoteView.setOnClickPendingIntent(
            R.id.cta1,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA1, context)
        )

        val bundleCTA2 = extras.clone() as Bundle
        bundleCTA2.putBoolean("cta2", true)
        bundleCTA2.putString(Constants.DEEP_LINK_KEY,deepLinkList.get(1))
        bundleCTA2.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 2 + "_" + deepLinkList.get(1))
        remoteView.setOnClickPendingIntent(
            R.id.cta2,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA2, context)
        )

        val bundleCTA3 = extras.clone() as Bundle
        bundleCTA3.putBoolean("cta3", true)
        bundleCTA3.putString(Constants.DEEP_LINK_KEY, deepLinkList.get(2))
        bundleCTA3.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 3 + "_" + deepLinkList.get(2))
        remoteView.setOnClickPendingIntent(
            R.id.cta3,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA3, context)
        )

        if(deepLinkList.size > 3) {
            val bundleCTA4 = extras.clone() as Bundle
            bundleCTA4.putBoolean("cta4", true)
            bundleCTA4.putString(Constants.DEEP_LINK_KEY, deepLinkList.get(3))
            bundleCTA4.putString(
                Constants.KEY_C2A,
                PTConstants.PT_5CTA_C2A_KEY + 4 + "_" + deepLinkList.get(3)
            )
            remoteView.setOnClickPendingIntent(
                R.id.cta4,
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA4, context)
            )
        }

        if(deepLinkList.size > 4) {
            val bundleCTA5 = extras.clone() as Bundle
            bundleCTA5.putBoolean("cta5", true)
            bundleCTA5.putString(Constants.DEEP_LINK_KEY, deepLinkList.get(4))
            bundleCTA5.putString(
                Constants.KEY_C2A,
                PTConstants.PT_5CTA_C2A_KEY + 5 + "_" + deepLinkList.get(4)
            )
            remoteView.setOnClickPendingIntent(
                R.id.cta5,
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA5, context)
            )
        }

        if (imageCounter > 2) {
            PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.")
        }
    }

    /**
     * Returns total number of five icon URL's which does not convert to bitmap
     */
    internal fun getUnloadedFiveIconsCount(): Int {
        return imageCounter
    }
}