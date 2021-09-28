package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class FiveIconContentView(context: Context,renderer: TemplateRenderer):
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
       if (imageCounter > 2) {
           PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.")
       }
   }
}