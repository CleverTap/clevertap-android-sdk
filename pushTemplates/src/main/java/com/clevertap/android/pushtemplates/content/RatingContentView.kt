package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

class RatingContentView(context: Context, renderer: TemplateRenderer):
    BigImageContentView(context,R.layout.rating,renderer) {

    init {
        setCustomContentViewBasicKeys()
        setCustomContentViewTitle(renderer.pt_title)
        setCustomContentViewMessage(renderer.pt_msg)
        setCustomContentViewMessageSummary(renderer.pt_msg_summary)
        setCustomContentViewTitleColour(renderer.pt_title_clr)
        setCustomContentViewMessageColour(renderer.pt_msg_clr)
        setCustomContentViewExpandedBackgroundColour(renderer.pt_bg)
        setCustomContentViewBigImage(renderer.pt_big_img)
        setCustomContentViewLargeIcon(renderer.pt_large_icon)
        setCustomContentViewSmallIcon()
        setCustomContentViewDotSep()

        //Set rating stars
        remoteView.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)

        remoteView.setOnClickPendingIntent(R.id.star1, PendingIntentFactory().getPendingIntent(context,
            1, Bundle(),false,RATING_NOTIFICATION_CLICK1_PENDING_INTENT,renderer))//TODO Check notifId and extras here)
        remoteView.setOnClickPendingIntent(R.id.star2, PendingIntentFactory().getPendingIntent(context,
            1, Bundle(),false,RATING_NOTIFICATION_CLICK2_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star3, PendingIntentFactory().getPendingIntent(context,
            1, Bundle(),false,RATING_NOTIFICATION_CLICK3_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star4, PendingIntentFactory().getPendingIntent(context,
            1, Bundle(),false,RATING_NOTIFICATION_CLICK4_PENDING_INTENT,renderer))
        remoteView.setOnClickPendingIntent(R.id.star5, PendingIntentFactory().getPendingIntent(context,
            1, Bundle(),false,RATING_NOTIFICATION_CLICK5_PENDING_INTENT,renderer))
    }

}