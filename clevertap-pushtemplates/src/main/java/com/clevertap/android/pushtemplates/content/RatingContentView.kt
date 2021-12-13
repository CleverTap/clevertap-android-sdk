package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

class RatingContentView(context: Context, renderer: TemplateRenderer, extras: Bundle):
    BigImageContentView(context,renderer,R.layout.rating) {

    init {
        //Set rating stars
        remoteView.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)

        remoteView.setOnClickPendingIntent(R.id.star1, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false,RATING_CLICK1_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star2, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false,RATING_CLICK2_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star3, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false,RATING_CLICK3_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star4, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false,RATING_CLICK4_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star5, PendingIntentFactory.getPendingIntent(context,
            renderer.notificationId, extras,false,RATING_CLICK5_PENDING_INTENT,renderer))
    }

}