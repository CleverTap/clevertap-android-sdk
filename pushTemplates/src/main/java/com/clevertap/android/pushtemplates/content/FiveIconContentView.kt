package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class FiveIconContentView constructor(context: Context,
    renderer: TemplateRenderer,
    extras: Bundle
):
    ContentView(context, R.layout.five_cta, renderer) {

   init {
       if (renderer.pt_title == null || renderer.pt_title!!.isEmpty()) {
           renderer.pt_title = Utils.getApplicationName(context)
       }
       setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
       var imageCounter = 0
       for (imageKey in renderer.imageList!!.indices) {
           if (imageKey == 0) {
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

       remoteView.setOnClickPendingIntent(R.id.cta1, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CTA1_PENDING_INTENT,renderer))
       remoteView.setOnClickPendingIntent(R.id.cta2, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CTA2_PENDING_INTENT,renderer))

       remoteView.setOnClickPendingIntent(R.id.cta3, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CTA3_PENDING_INTENT,renderer))

       remoteView.setOnClickPendingIntent(R.id.cta4, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CTA4_PENDING_INTENT,renderer))

       remoteView.setOnClickPendingIntent(R.id.cta5, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CTA5_PENDING_INTENT,renderer))

       remoteView.setOnClickPendingIntent(R.id.close, PendingIntentFactory.getPendingIntent(context,
           renderer.notificationId, extras,false, FIVE_ICON_CLOSE_PENDING_INTENT,renderer))


       if (imageCounter > 2) {
           PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.")
       }
   }
}