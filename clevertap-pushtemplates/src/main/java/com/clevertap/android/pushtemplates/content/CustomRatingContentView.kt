package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

/**
 * Expanded view for the pt_custom_rating template.
 *
 * Renders the row in its unselected state. With a submit button configured nothing here raises an
 * event or opens a deep link — the rating is only submitted once the user taps Submit. Without one
 * (FR-DSH-09) the position tap submits directly.
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
        //
        // Static images only. pt_gif is not supported on this template, so the animated path is
        // skipped outright rather than entered and allowed to fall through to the still image —
        // that route decodes every frame first, which is the most expensive thing this template
        // could do for a result it is going to discard.
        if (!data.mediaData.gif.url.isNullOrBlank()) {
            PTLog.debug(
                "${PTConstants.PT_GIF} is not supported by pt_custom_rating, " +
                        "rendering ${PTConstants.PT_BIG_IMG} instead"
            )
        }
        setCustomContentViewBigImage(
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
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
            val intent = if (data.hasSubmitButton) {
                // Deferred submit: the tap only moves the selection.
                PendingIntentFactory.getCustomRatingPositionIntent(
                    context, renderer.notificationId, extras, position, renderer.config
                )
            } else {
                // No submit button (FR-DSH-09): the tap raises the event and opens pt_dl{n} in one
                // step. Each position needs its own request-code slot or they overwrite each other.
                PendingIntentFactory.getCustomRatingSubmitIntent(
                    context, renderer.notificationId, extras, position, renderer.config, position
                )
            }
            remoteView.setOnClickPendingIntent(CustomRatingRowRenderer.cellViewId(position), intent)
        }

        if (data.hasSubmitButton) {
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
    }

    companion object {

        const val NO_SELECTION = 0
    }
}
