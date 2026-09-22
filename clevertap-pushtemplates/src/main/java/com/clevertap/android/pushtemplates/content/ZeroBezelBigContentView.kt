package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.view.View
import com.clevertap.android.pushtemplates.ImageBorderData
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData
import com.clevertap.android.pushtemplates.usesNativeImageStyling

internal class ZeroBezelBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ZeroBezelTemplateData
) :
    ActionButtonsContentView(context, renderer, R.layout.zero_bezel) {

    /** Zero Bezel hands the picture the whole surface, which comes out taller than the standard image area. */
    override val imageStyleReferenceDimen: Int
        get() = R.dimen.pt_image_style_reference_zero_bezel

    init {
        setCustomContentViewBasicKeys(data.baseContent.textData.subtitle, data.baseContent.colorData.metaColor)
        setCustomContentViewTitle(data.baseContent.textData.title)
        setCustomContentViewMessage(data.baseContent.textData.message)
        setCustomContentViewMessageSummary(data.baseContent.textData.messageSummary)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)

        // The scrim stays the separate view. Baking it into the bitmap put the gradient on the
        // source image, which the tray then crops for a CENTER_CROP picture - taking the dark end
        // of the gradient with it and leaving the text on a bright background. The view always
        // covers the area that is actually displayed.
        // The corner belongs to the surface for BOTH scale types - in Zero Bezel the surface is the
        // card the user sees, so it rounds whether or not the picture happens to fill it.
        //
        // CENTER_CROP keeps the full styling: the picture is the surface, so the ring the views
        // draw around it already sits on the card's edge. FIT_CENTER gets none of it - a baked
        // curve would be measured against the bitmap's height rather than the surface's and would
        // sit inside the card's corner as a second, mismatched outline, and a baked ring would
        // trace the letterboxed picture instead of the card.
        val mediaBorder =
            if (data.mediaData.scaleType.usesNativeImageStyling()) data.mediaData.imageBorderData
            else ImageBorderData()

        applyZeroBezelSurfaceStyling(
            remoteView, data.mediaData.imageBorderData, data.mediaData.scaleType
        )

        val isMediaLoaded = setCustomContentViewMedia(
            R.layout.image_view_dynamic_relative,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames,
            mediaBorder
        )
        if (!isMediaLoaded) {
            PTLog.debug("Download failed for all media in ZeroBezel Expanded Notification. Not showing the image")
            remoteView.setViewVisibility(R.id.zero_bezel_scrim, View.GONE)
        }
    }
}