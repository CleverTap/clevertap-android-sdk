package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

/**
 * Expanded view for the pt_custom_rating template.
 *
 * Renders the row in its unselected state — the rating is only submitted once the user taps the
 * submit button, so nothing here raises an event or opens a deep link.
 *
 * Separate from [RatingContentView] by design: the Classic pt_rating template is frozen and shares
 * neither view ids nor intents with this one.
 */
internal class CustomRatingContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: CustomRatingTemplateData,
    extras: Bundle,
) : ContentView(context, R.layout.custom_rating, renderer.templateMediaManager) {

    /**
     * Greyscale copies handed over by the row renderer. RemoteViews parcels bitmaps during
     * `notify()`, so the caller must recycle these only after the notification has been posted.
     */
    val ownedBitmaps: List<Bitmap>

    init {
        val baseContent = data.baseContent
        setCustomContentViewBasicKeys(
            baseContent.textData.subtitle,
            baseContent.colorData.metaColor
        )
        setCustomContentViewTitle(baseContent.textData.title)
        setCustomContentViewMessage(baseContent.textData.message)
        setCustomBackgroundColour(baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewMessageSummary(baseContent.textData.messageSummary)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
        // Banner-height media: the row and the submit button have to stay above the fold.
        setCustomContentViewMedia(
            R.layout.image_view_rating_banner,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames,
            data.mediaData.imageBorderData
        )
        setCustomContentViewLargeIcon(baseContent.iconData.largeIcon)

        // NO_SELECTION: the row opens with every position unselected.
        ownedBitmaps = CustomRatingRowRenderer.renderRow(
            remoteViews = remoteView,
            data = data,
            mediaManager = templateMediaManager,
            selectedPosition = NO_SELECTION
        )

        for (position in 1..data.ratingCount) {
            remoteView.setOnClickPendingIntent(
                CustomRatingRowRenderer.cellViewId(position),
                PendingIntentFactory.getCustomRatingPositionIntent(
                    context, renderer.notificationId, extras, position, renderer.config
                )
            )
        }

        // Attached even with nothing selected so the tap is swallowed by the receiver instead of
        // falling through to the body-tap intent behind it. The handler returns early on
        // NO_SELECTION (FR-AND-04).
        remoteView.setOnClickPendingIntent(
            R.id.custom_rating_cta,
            PendingIntentFactory.getCustomRatingSubmitIntent(
                context, renderer.notificationId, extras, NO_SELECTION, renderer.config
            )
        )
    }

    companion object {

        const val NO_SELECTION = 0
    }
}
