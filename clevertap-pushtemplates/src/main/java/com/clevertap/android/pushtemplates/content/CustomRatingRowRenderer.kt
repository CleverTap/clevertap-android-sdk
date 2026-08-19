package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.RatingStyleType
import com.clevertap.android.pushtemplates.TemplateDataFactory
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.media.TemplateMediaManager

/**
 * Draws the pt_custom_rating position row and submit button into a [RemoteViews].
 *
 * Both the initial render ([CustomRatingContentView]) and every re-render after a position tap
 * (PushTemplateReceiver) go through here, so the selected and unselected states cannot drift apart.
 *
 * Bitmaps returned by [renderRow] are derived copies that the caller owns. RemoteViews parcels
 * bitmaps during `notify()`, so they must only be recycled after the notification has been posted.
 */
internal object CustomRatingRowRenderer {

    /** View ids for positions 1..5, in order. */
    private val POSITION_VIEW_IDS = intArrayOf(
        R.id.custom_rating_pos1,
        R.id.custom_rating_pos2,
        R.id.custom_rating_pos3,
        R.id.custom_rating_pos4,
        R.id.custom_rating_pos5,
    )

    /** Fixed canvas for the generated submit-button background; it is stretched with fitXY. */
    private const val CTA_BITMAP_WIDTH = 400
    private const val CTA_BITMAP_HEIGHT = 100

    /**
     * Re-parses the payload out of [extras] and renders the row for [selectedPosition]. Used by the
     * receiver, which only has the notification's extras to work from.
     *
     * @return null when the payload no longer describes a renderable custom rating template.
     */
    @JvmStatic
    fun renderRowFromExtras(
        context: Context,
        extras: Bundle,
        remoteViews: RemoteViews,
        mediaManager: TemplateMediaManager,
        selectedPosition: Int
    ): List<Bitmap>? {
        val data = TemplateDataFactory.createTemplateData(
            TemplateType.CUSTOM_RATING,
            extras,
            Utils.isDarkMode(context),
            context.getString(R.string.pt_big_image_alt)
        ) { Utils.getNotificationIds(context) } as? CustomRatingTemplateData ?: return null

        if (!isRenderable(data)) return null
        return renderRow(remoteViews, data, mediaManager, selectedPosition)
    }

    /**
     * A payload the row renderer can draw: a known style, and at least [PTConstants.PT_RATING_COUNT_MIN]
     * positions carrying usable config. Anything less falls back to the Basic template (R-22).
     */
    @JvmStatic
    fun isRenderable(data: CustomRatingTemplateData): Boolean {
        val style = data.ratingStyle
        if (style == null) {
            PTLog.debug("${PTConstants.PT_RATING_STYLE} is missing or invalid, falling back to the basic template")
            return false
        }
        if (data.ratingCount < PTConstants.PT_RATING_COUNT_MIN) {
            PTLog.debug("${PTConstants.PT_RATING_COUNT} is missing or below ${PTConstants.PT_RATING_COUNT_MIN}, falling back to the basic template")
            return false
        }
        if (style == RatingStyleType.TEXT) {
            // Text style is specified in the PRD but not yet rendered by this SDK. Rather than draw
            // an icon row a text campaign never asked for, degrade to the basic template.
            PTLog.debug("${PTConstants.PT_RATING_STYLE}=text is not supported yet, falling back to the basic template")
            return false
        }
        if (data.renderablePositionCount < PTConstants.PT_RATING_COUNT_MIN) {
            PTLog.debug("Only ${data.renderablePositionCount} rating positions are configured, falling back to the basic template")
            return false
        }
        if (data.ctaData.label == null || data.ctaData.deepLink == null) {
            PTLog.debug("${PTConstants.PT_RATING_CTA_LABEL} and ${PTConstants.PT_RATING_CTA_DL} are both required, falling back to the basic template")
            return false
        }
        return true
    }

    /**
     * Renders positions 1..[CustomRatingTemplateData.ratingCount] with [selectedPosition] highlighted,
     * hides the unused slots, and styles the submit button.
     *
     * @param selectedPosition 1-based selected position, or 0 for "nothing selected yet".
     * @return derived bitmaps the caller must recycle after `notify()`.
     */
    @JvmStatic
    fun renderRow(
        remoteViews: RemoteViews,
        data: CustomRatingTemplateData,
        mediaManager: TemplateMediaManager,
        selectedPosition: Int
    ): List<Bitmap> {
        val ownedBitmaps = mutableListOf<Bitmap>()

        for (index in POSITION_VIEW_IDS.indices) {
            val viewId = POSITION_VIEW_IDS[index]
            val position = index + 1
            if (position > data.ratingCount) {
                remoteViews.setViewVisibility(viewId, View.GONE)
                continue
            }
            remoteViews.setViewVisibility(viewId, View.VISIBLE)
            renderIcon(
                remoteViews = remoteViews,
                viewId = viewId,
                data = data,
                position = position,
                isSelected = position == selectedPosition,
                mediaManager = mediaManager,
                ownedBitmaps = ownedBitmaps
            )
        }

        renderCta(remoteViews, data)
        return ownedBitmaps
    }

    /**
     * Draws one position. Positions are single-select, so only the selected one shows its selected
     * asset; every other position shows the unselected asset greyed out.
     *
     * A position whose asset failed to download falls back to the built-in star pair (R-21) rather
     * than leaving an empty slot.
     */
    private fun renderIcon(
        remoteViews: RemoteViews,
        viewId: Int,
        data: CustomRatingTemplateData,
        position: Int,
        isSelected: Boolean,
        mediaManager: TemplateMediaManager,
        ownedBitmaps: MutableList<Bitmap>
    ) {
        val positionData = data.positions.getOrNull(position - 1)
        val url = if (isSelected) {
            positionData?.selectedIconUrl ?: positionData?.iconUrl
        } else {
            positionData?.iconUrl
        }

        // Bitmaps from the media manager are cache-owned and must never be recycled here.
        val cached = url?.let { mediaManager.getImageBitmap(it) }
        if (cached == null) {
            PTLog.debug("Rating icon for position $position could not be loaded, substituting the built-in star")
            remoteViews.setImageViewResource(
                viewId,
                if (isSelected) R.drawable.pt_star_filled else R.drawable.pt_star_outline
            )
            applyTint(remoteViews, viewId, data, isSelected)
            return
        }

        if (isSelected) {
            remoteViews.setImageViewBitmap(viewId, cached)
        } else {
            // Greyscale is a derived copy, so this one is ours to recycle.
            val grey = Utils.toGreyscale(cached)
            if (grey != null) {
                remoteViews.setImageViewBitmap(viewId, grey)
                ownedBitmaps.add(grey)
            } else {
                remoteViews.setImageViewBitmap(viewId, cached)
            }
        }
        applyTint(remoteViews, viewId, data, isSelected)
    }

    /**
     * Applies pt_rating_icon_clr / pt_rating_icon_sel_clr. Only monochrome and template assets are
     * affected in any visible way, which matches the dashboard's own guidance for these keys.
     */
    private fun applyTint(
        remoteViews: RemoteViews,
        viewId: Int,
        data: CustomRatingTemplateData,
        isSelected: Boolean
    ) {
        val colorString = if (isSelected) data.selectedIconColor else data.iconColor
        val tint = colorString?.let { Utils.getColourOrNull(it) } ?: return
        remoteViews.setInt(viewId, "setColorFilter", tint)
    }

    /**
     * Styles the submit button. The fill, border and corner radius are baked into a bitmap because a
     * RemoteViews cannot build a drawable; the label sits on top of it.
     */
    private fun renderCta(remoteViews: RemoteViews, data: CustomRatingTemplateData) {
        val cta = data.ctaData
        remoteViews.setViewVisibility(R.id.custom_rating_cta, View.VISIBLE)
        cta.label?.let { remoteViews.setTextViewText(R.id.custom_rating_cta_label, it) }
        cta.textColor?.let { color ->
            Utils.getColourOrNull(color)?.let {
                remoteViews.setTextColor(R.id.custom_rating_cta_label, it)
            }
        }

        val fill = cta.backgroundColor?.let { Utils.getColourOrNull(it) }
        val border = cta.borderColor?.let { Utils.getColourOrNull(it) }
        if (fill == null && border == null) return

        // The radius arrives in dp but the canvas is a fixed pixel size, so scale it to keep the
        // rendered curve proportional to the button the user actually sees.
        val radiusPx = cta.cornerRadiusDp.toFloat() *
                (CTA_BITMAP_HEIGHT.toFloat() / DEFAULT_CTA_HEIGHT_DP)

        val background = NotificationBitmapUtils.createSolidBitmap(
            bgColor = fill ?: Color.TRANSPARENT,
            borderColor = border,
            width = CTA_BITMAP_WIDTH,
            height = CTA_BITMAP_HEIGHT,
            cornerRadius = radiusPx,
            borderWidth = if (border != null) PTConstants.PT_BTN_BORDER_WIDTH_DEFAULT else null
        )
        remoteViews.setImageViewBitmap(R.id.custom_rating_cta_bg, background)
        // The generated bitmap already carries the fill, so drop the placeholder behind it or its
        // square corners show through the rounded ones.
        remoteViews.setInt(R.id.custom_rating_cta, "setBackgroundColor", Color.TRANSPARENT)
    }

    /** Nominal on-screen height of the submit button, used to scale the dp corner radius. */
    private const val DEFAULT_CTA_HEIGHT_DP = 44f
}
