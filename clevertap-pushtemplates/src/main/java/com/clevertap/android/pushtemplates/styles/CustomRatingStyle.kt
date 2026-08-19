package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.CUSTOM_RATING_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.CustomRatingContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView

/**
 * Notification style for the pt_custom_rating template.
 *
 * The collapsed view is the standard small content view — the rating row and submit button are
 * interactive and only belong in the expanded view (FR-AND-02).
 */
internal class CustomRatingStyle(
    private val data: CustomRatingTemplateData,
    renderer: TemplateRenderer,
    private var extras: Bundle
) : Style(data.baseContent, renderer) {

    /** Greyscale copies the caller must recycle once the notification has been posted. */
    var ownedBitmaps: List<android.graphics.Bitmap> = emptyList()
        private set

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer, data.baseContent).remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        val contentView = CustomRatingContentView(context, renderer, data, extras)
        ownedBitmaps = contentView.ownedBitmaps
        return contentView.remoteView
    }

    /** Body tap opens pt_default_dl and never raises Rating Submitted (FR-AND-04). */
    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, false,
            CUSTOM_RATING_CONTENT_PENDING_INTENT, data.defaultDeepLink
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }
}
