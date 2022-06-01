package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

class FiveIconSmallContentView constructor(
    context: Context,
    renderer: TemplateRenderer,
    extras: Bundle
) : ContentView(context, R.layout.five_cta_collapsed, renderer) {

    private var imageCounter: Int = 0

    init {
        if (renderer.pt_title == null || renderer.pt_title!!.isEmpty()) {
            renderer.pt_title = Utils.getApplicationName(context)
        }
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        for (imageKey in renderer.imageList!!.indices) {
            if (imageKey == 0) {
                remoteView.setViewVisibility(R.id.cta1, View.VISIBLE)
                Utils.loadImageURLIntoRemoteView(
                    R.id.cta1,
                    renderer.imageList!![imageKey],
                    remoteView
                )
                if (Utils.getFallback()) {
                    remoteView.setViewVisibility(R.id.cta1, View.GONE)
                    imageCounter++
                }
            } else if (imageKey == 1) {
                remoteView.setViewVisibility(R.id.cta2, View.VISIBLE)
                Utils.loadImageURLIntoRemoteView(
                    R.id.cta2,
                    renderer.imageList!![imageKey],
                    remoteView
                )
                if (Utils.getFallback()) {
                    imageCounter++
                    remoteView.setViewVisibility(R.id.cta2, View.GONE)
                }
            } else if (imageKey == 2) {
                remoteView.setViewVisibility(R.id.cta3, View.VISIBLE)
                Utils.loadImageURLIntoRemoteView(
                    R.id.cta3,
                    renderer.imageList!![imageKey],
                    remoteView
                )
                if (Utils.getFallback()) {
                    imageCounter++
                    remoteView.setViewVisibility(R.id.cta3, View.GONE)
                }
            } else if (imageKey == 3) {
                remoteView.setViewVisibility(R.id.cta4, View.VISIBLE)
                Utils.loadImageURLIntoRemoteView(
                    R.id.cta4,
                    renderer.imageList!![imageKey],
                    remoteView
                )
                if (Utils.getFallback()) {
                    imageCounter++
                    remoteView.setViewVisibility(R.id.cta4, View.GONE)
                }
            } else if (imageKey == 4) {
                remoteView.setViewVisibility(R.id.cta5, View.VISIBLE)
                Utils.loadImageURLIntoRemoteView(
                    R.id.cta5,
                    renderer.imageList!![imageKey],
                    remoteView
                )
                if (Utils.getFallback()) {
                    imageCounter++
                    remoteView.setViewVisibility(R.id.cta5, View.GONE)
                }
            }
        }
        Utils.loadImageRidIntoRemoteView(R.id.close, R.drawable.pt_close, remoteView)

        extras.putInt(PTConstants.PT_NOTIF_ID, renderer.notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)

        val bundleCTA1 = extras.clone() as Bundle
        bundleCTA1.putBoolean("cta1", true)
        bundleCTA1.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList?.get(0))
        bundleCTA1.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 1 + "_" + renderer.deepLinkList?.get(0))
        remoteView.setOnClickPendingIntent(
            R.id.cta1,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA1, context)
        )

        val bundleCTA2 = extras.clone() as Bundle
        bundleCTA2.putBoolean("cta2", true)
        bundleCTA2.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList?.get(1))
        bundleCTA2.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 2 + "_" + renderer.deepLinkList?.get(1))
        remoteView.setOnClickPendingIntent(
            R.id.cta2,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA2, context)
        )

        val bundleCTA3 = extras.clone() as Bundle
        bundleCTA3.putBoolean("cta3", true)
        bundleCTA3.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList?.get(2))
        bundleCTA3.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + 3 + "_" + renderer.deepLinkList?.get(2))
        remoteView.setOnClickPendingIntent(
            R.id.cta3,
            LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA3, context)
        )

        if(renderer.deepLinkList != null && renderer.deepLinkList!!.size > 3) {
            val bundleCTA4 = extras.clone() as Bundle
            bundleCTA4.putBoolean("cta4", true)
            bundleCTA4.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList?.get(3))
            bundleCTA4.putString(
                Constants.KEY_C2A,
                PTConstants.PT_5CTA_C2A_KEY + 4 + "_" + renderer.deepLinkList?.get(3)
            )
            remoteView.setOnClickPendingIntent(
                R.id.cta4,
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA4, context)
            )
        }

        if(renderer.deepLinkList != null && renderer.deepLinkList!!.size > 4) {
            val bundleCTA5 = extras.clone() as Bundle
            bundleCTA5.putBoolean("cta5", true)
            bundleCTA5.putString(Constants.DEEP_LINK_KEY, renderer.deepLinkList?.get(4))
            bundleCTA5.putString(
                Constants.KEY_C2A,
                PTConstants.PT_5CTA_C2A_KEY + 5 + "_" + renderer.deepLinkList?.get(4)
            )
            remoteView.setOnClickPendingIntent(
                R.id.cta5,
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA5, context)
            )
        }

        remoteView.setOnClickPendingIntent(
            R.id.close, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, FIVE_ICON_CLOSE_PENDING_INTENT, renderer
            )
        )


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