package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.R.drawable
import com.clevertap.android.pushtemplates.R.id
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

class RatingContentView(context: Context, renderer: TemplateRenderer, extras: Bundle) :
    BigImageContentView(context, renderer, R.layout.rating) {

    init {
        //Set rating stars
        remoteView.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
        remoteView.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)

        remoteView.setOnClickPendingIntent(
            R.id.star1, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK1_PENDING_INTENT, renderer
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star2, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK2_PENDING_INTENT, renderer
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star3, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK3_PENDING_INTENT, renderer
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star4, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK4_PENDING_INTENT, renderer
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.star5, PendingIntentFactory.getPendingIntent(
                context,
                renderer.notificationId, extras, false, RATING_CLICK5_PENDING_INTENT, renderer
            )
        )

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            remoteView.setViewVisibility(R.id.tVRatingConfirmation, View.VISIBLE)
            extras.putInt(PTConstants.PT_NOTIF_ID, renderer.notificationId)
            remoteView.setOnClickPendingIntent(
                R.id.tVRatingConfirmation,
                LaunchPendingIntentFactory.getActivityIntent(extras, context)
            )
        } else {
            remoteView.setViewVisibility(R.id.tVRatingConfirmation, View.GONE)
        }
        val extrasFrom = extras.getString(Constants.EXTRAS_FROM, "")
        if (extrasFrom == "PTReceiver") {
            if (1 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_outline)
            }
            if (2 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star2, drawable.pt_star_outline)
            }
            if (3 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star3, drawable.pt_star_outline)
            }
            if (4 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star4, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star4, drawable.pt_star_outline)
            }
            if (5 == extras.getInt(PTConstants.KEY_CLICKED_STAR, 0)) {
                remoteView.setImageViewResource(id.star1, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star2, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star3, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star4, drawable.pt_star_filled)
                remoteView.setImageViewResource(id.star5, drawable.pt_star_filled)
            } else {
                remoteView.setImageViewResource(id.star5, drawable.pt_star_outline)
            }
        }
    }
}