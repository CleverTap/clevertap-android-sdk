package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.TypedValue
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

    /** Cell containers for positions 1..5, in order. The whole cell is the tap target. */
    private val CELL_VIEW_IDS = intArrayOf(
        R.id.custom_rating_cell1,
        R.id.custom_rating_cell2,
        R.id.custom_rating_cell3,
        R.id.custom_rating_cell4,
        R.id.custom_rating_cell5,
    )

    /** Icon-style view for positions 1..5, in order. */
    private val ICON_VIEW_IDS = intArrayOf(
        R.id.custom_rating_pos1,
        R.id.custom_rating_pos2,
        R.id.custom_rating_pos3,
        R.id.custom_rating_pos4,
        R.id.custom_rating_pos5,
    )

    /** Text-style label for positions 1..5, in order. */
    private val LABEL_VIEW_IDS = intArrayOf(
        R.id.custom_rating_label1,
        R.id.custom_rating_label2,
        R.id.custom_rating_label3,
        R.id.custom_rating_label4,
        R.id.custom_rating_label5,
    )

    /** Drawn chip background behind the label, used only below Android 12. */
    private val CHIP_VIEW_IDS = intArrayOf(
        R.id.custom_rating_chip1,
        R.id.custom_rating_chip2,
        R.id.custom_rating_chip3,
        R.id.custom_rating_chip4,
        R.id.custom_rating_chip5,
    )

    /** Fixed canvas for the generated submit-button background; it is stretched with fitXY. */
    private const val CTA_BITMAP_WIDTH = 400
    private const val CTA_BITMAP_HEIGHT = 100

    /** Fixed canvas for a generated chip background, at the chip's own 4:1-ish proportions. */
    private const val CHIP_BITMAP_WIDTH = 200
    private const val CHIP_BITMAP_HEIGHT = 64

    /** Nominal on-screen height of the submit button, used to scale the dp corner radius. */
    private const val DEFAULT_CTA_HEIGHT_DP = 44f

    /** Nominal on-screen height of a text chip, matching R.dimen.custom_rating_chip_height. */
    private const val CHIP_HEIGHT_DP = 32f

    /** Corner radius of the chip, matching R.dimen.custom_rating_chip_radius. */
    private const val CHIP_RADIUS_DP = 16f

    /**
     * Label text size in sp, by position count. Four and five positions leave a chip barely wider
     * than a word, so the text steps down to buy roughly one more character; the composer's own
     * per-count character limits do the rest.
     */
    private const val LABEL_TEXT_SIZE_SP = 12f
    private const val LABEL_TEXT_SIZE_SP_DENSE = 11f

    /**
     * Characters a label can show before it is ellipsised, by position count. Purely advisory — the
     * device cannot measure text before it is drawn (so the row always ellipsises as a safety net),
     * but logging the overflow is what tells a campaign author their label never made it on screen.
     */
    private val LABEL_BUDGET_BY_COUNT = mapOf(2 to 15, 3 to 10, 4 to 7, 5 to 5)

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
     * A text row missing any label is drawn as the built-in star row instead (R-23) — a gap where a
     * chip should be reads as a broken notification, five stars do not.
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

        val drawAsText = data.ratingStyle == RatingStyleType.TEXT && !data.hasIncompleteTextRow
        if (data.hasIncompleteTextRow) {
            PTLog.debug(
                "${PTConstants.PT_RATING_LABEL_PREFIX}{n} is missing for one or more positions, " +
                        "falling back to the built-in stars for the whole row"
            )
        }

        for (index in CELL_VIEW_IDS.indices) {
            val position = index + 1
            if (position > data.ratingCount) {
                remoteViews.setViewVisibility(CELL_VIEW_IDS[index], View.GONE)
                continue
            }
            remoteViews.setViewVisibility(CELL_VIEW_IDS[index], View.VISIBLE)

            val isSelected = position == selectedPosition
            if (drawAsText) {
                showTextCell(remoteViews, index)
                renderLabel(remoteViews, index, data, isSelected, ownedBitmaps)
            } else {
                showIconCell(remoteViews, index)
                renderIcon(remoteViews, ICON_VIEW_IDS[index], data, position, isSelected, mediaManager, ownedBitmaps)
            }
        }

        renderCta(remoteViews, data)
        return ownedBitmaps
    }

    private fun showIconCell(remoteViews: RemoteViews, index: Int) {
        remoteViews.setViewVisibility(ICON_VIEW_IDS[index], View.VISIBLE)
        remoteViews.setViewVisibility(LABEL_VIEW_IDS[index], View.GONE)
        remoteViews.setViewVisibility(CHIP_VIEW_IDS[index], View.GONE)
    }

    private fun showTextCell(remoteViews: RemoteViews, index: Int) {
        remoteViews.setViewVisibility(ICON_VIEW_IDS[index], View.GONE)
        remoteViews.setViewVisibility(LABEL_VIEW_IDS[index], View.VISIBLE)
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
     * Draws one text chip: the label, its colour, and the chip fill behind it.
     *
     * The fill takes one of two routes, both of which the SDK already uses elsewhere. From Android 12
     * the shipped rounded drawable is tinted in place, which draws no bitmap at all and so costs
     * nothing against the notification's image-memory budget. Below that a bitmap is generated the
     * same way the submit button's background is, because a RemoteViews cannot recolour a drawable.
     */
    private fun renderLabel(
        remoteViews: RemoteViews,
        index: Int,
        data: CustomRatingTemplateData,
        isSelected: Boolean,
        ownedBitmaps: MutableList<Bitmap>
    ) {
        val labelViewId = LABEL_VIEW_IDS[index]
        val label = data.positions.getOrNull(index)?.label.orEmpty()
        remoteViews.setTextViewText(labelViewId, label)
        remoteViews.setContentDescription(labelViewId, label)
        remoteViews.setTextViewTextSize(
            labelViewId, TypedValue.COMPLEX_UNIT_SP, labelTextSizeSp(data.ratingCount)
        )
        warnIfLabelOverflows(label, index + 1, data.ratingCount)

        resolveLabelColor(data, isSelected)?.let { remoteViews.setTextColor(labelViewId, it) }

        val fill = (if (isSelected) data.selectedIconColor else data.iconColor)
            ?.let { Utils.getColourOrNull(it) }
        if (fill == null) {
            // No chip colour configured: the label sits on the notification background, and the
            // shipped drawable would otherwise show as a white pill.
            remoteViews.setViewVisibility(CHIP_VIEW_IDS[index], View.GONE)
            remoteViews.setInt(labelViewId, "setBackgroundResource", 0)
            return
        }

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            remoteViews.setViewVisibility(CHIP_VIEW_IDS[index], View.GONE)
            remoteViews.setInt(labelViewId, "setBackgroundResource", R.drawable.pt_rating_chip)
            remoteViews.setColorStateList(
                labelViewId, "setBackgroundTintList", ColorStateList.valueOf(fill)
            )
            return
        }

        remoteViews.setInt(labelViewId, "setBackgroundResource", 0)
        remoteViews.setViewVisibility(CHIP_VIEW_IDS[index], View.VISIBLE)
        val chip = NotificationBitmapUtils.createSolidBitmap(
            bgColor = fill,
            borderColor = null,
            width = CHIP_BITMAP_WIDTH,
            height = CHIP_BITMAP_HEIGHT,
            cornerRadius = CHIP_RADIUS_DP * (CHIP_BITMAP_HEIGHT / CHIP_HEIGHT_DP)
        )
        remoteViews.setImageViewBitmap(CHIP_VIEW_IDS[index], chip)
        ownedBitmaps.add(chip)
    }

    /**
     * pt_rating_label_clr / pt_rating_label_sel_clr when set, otherwise the notification's own
     * message and title colours, so a chip is legible without the campaign configuring anything.
     */
    private fun resolveLabelColor(data: CustomRatingTemplateData, isSelected: Boolean): Int? {
        val configured = if (isSelected) data.selectedLabelColor else data.labelColor
        val fallback = with(data.baseContent.colorData) { if (isSelected) titleColor else messageColor }
        return (configured ?: fallback)?.let { Utils.getColourOrNull(it) }
    }

    private fun labelTextSizeSp(ratingCount: Int): Float =
        if (ratingCount >= 4) LABEL_TEXT_SIZE_SP_DENSE else LABEL_TEXT_SIZE_SP

    private fun warnIfLabelOverflows(label: String, position: Int, ratingCount: Int) {
        val budget = LABEL_BUDGET_BY_COUNT[ratingCount] ?: return
        if (label.length > budget) {
            PTLog.debug(
                "${PTConstants.PT_RATING_LABEL_PREFIX}$position is ${label.length} characters; " +
                        "about $budget fit across $ratingCount positions, the rest is ellipsised"
            )
        }
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

    /** Cell id for a 1-based [position], used by the callers that attach the tap intents. */
    @JvmStatic
    fun cellViewId(position: Int): Int =
        CELL_VIEW_IDS[(position - 1).coerceIn(0, CELL_VIEW_IDS.lastIndex)]

    /** Hides the interactive parts of the row, leaving only the content block (confirmation state). */
    @JvmStatic
    fun hideInteractiveViews(remoteViews: RemoteViews) {
        remoteViews.setViewVisibility(R.id.custom_rating_row, View.GONE)
        remoteViews.setViewVisibility(R.id.custom_rating_cta, View.GONE)
    }
}
