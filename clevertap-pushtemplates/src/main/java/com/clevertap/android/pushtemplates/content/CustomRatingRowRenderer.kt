package com.clevertap.android.pushtemplates.content

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.RatingStyleType
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

    /**
     * Opacity the submit button is drawn at while no position is selected. Tapping it is already a
     * no-op (R-28); this is what tells the user that before they tap. Matches the 38% the platform
     * uses for disabled content, and mirrors the disabled Submit iOS renders (FR-IOS-01).
     */
    private const val CTA_DISABLED_ALPHA = 0.38f

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
        if (style == RatingStyleType.TEXT) {
            // Only the icon style ships. The text renderer below is left compiled and covered by its
            // own tests so turning the style back on is a one-line change rather than a rewrite.
            PTLog.debug(
                "${PTConstants.PT_RATING_STYLE} is \"$style\", which is not supported, " +
                        "falling back to the basic template"
            )
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
        selectedPosition: Int,
        iconMaxSidePx: Int = 0
    ): List<Bitmap> {
        val ownedBitmaps = mutableListOf<Bitmap>()

        val drawAsText = data.ratingStyle == RatingStyleType.TEXT && !data.hasIncompleteTextRow
        // R-23 substitutes the built-in stars for a broken text row, which makes it a scale too.
        val backfills = data.usesUniformScale || data.hasIncompleteTextRow
        if (data.hasIncompleteTextRow) {
            PTLog.debug(
                "${PTConstants.PT_RATING_LABEL_PREFIX}{n} is missing for one or more positions, " +
                        "falling back to the built-in stars for the whole row"
            )
        }

        // Resolved as a set, before anything is drawn.
        val rowArtwork = if (drawAsText) null else resolveRowArtwork(data, mediaManager)

        for (index in CELL_VIEW_IDS.indices) {
            val position = index + 1
            if (position > data.ratingCount) {
                remoteViews.setViewVisibility(CELL_VIEW_IDS[index], View.GONE)
                continue
            }
            remoteViews.setViewVisibility(CELL_VIEW_IDS[index], View.VISIBLE)

            // A uniform scale fills every position up to the tapped one; a row of distinct choices
            // highlights only the one that was picked (FR-AND-03).
            val isSelected =
                if (backfills) position <= selectedPosition else position == selectedPosition
            if (drawAsText) {
                showTextCell(remoteViews, index)
                renderLabel(remoteViews, index, data, isSelected, ownedBitmaps)
            } else {
                showIconCell(remoteViews, index)
                renderIcon(
                    remoteViews, ICON_VIEW_IDS[index], data, position, isSelected,
                    rowArtwork?.get(index), mediaManager, ownedBitmaps, iconMaxSidePx
                )
            }
        }

        renderCta(remoteViews, data, isEnabled = selectedPosition > 0)
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
     * The unselected artwork for every drawn position, or null when any single one of them failed.
     *
     * All-or-nothing on purpose. One substituted star among four downloaded icons reads as a broken
     * notification rather than a graceful fallback, and no tint can make the two match — the built-in
     * star is monochrome and takes a tint, the campaign's artwork is full colour and ignores it. The
     * whole row therefore falls back together, exactly as a text row does when a label is missing
     * (R-23), so the positions always look like each other.
     *
     * Looked up once and reused: [TemplateMediaManager] does not cache a URL that failed, so asking
     * a second time would download a second time.
     */
    private fun resolveRowArtwork(
        data: CustomRatingTemplateData,
        mediaManager: TemplateMediaManager
    ): List<Bitmap>? {
        val bitmaps = ArrayList<Bitmap>(data.ratingCount)
        var anyFailed = false
        for (position in 1..data.ratingCount) {
            val url = data.positions.getOrNull(position - 1)?.iconUrl
            // Keep going after the first failure so every bad key gets named, not just the first.
            val bitmap = url?.let { mediaManager.getImageBitmap(it) }
            if (bitmap == null) {
                PTLog.debug("${PTConstants.PT_RATING_ICON_PREFIX}$position could not be loaded")
                anyFailed = true
            } else {
                bitmaps.add(bitmap)
            }
        }
        if (anyFailed) {
            PTLog.debug("Falling back to the built-in stars for the whole row so the positions match")
            return null
        }
        return bitmaps
    }

    /**
     * Draws one position. Positions are single-select unless the row is a uniform scale, and either
     * way a position shows its selected artwork only when the selection reaches it.
     *
     * [artwork] is this position's unselected bitmap, already resolved by [resolveRowArtwork], or
     * null when the row is drawing the built-in stars instead.
     */
    private fun renderIcon(
        remoteViews: RemoteViews,
        viewId: Int,
        data: CustomRatingTemplateData,
        position: Int,
        isSelected: Boolean,
        artwork: Bitmap?,
        mediaManager: TemplateMediaManager,
        ownedBitmaps: MutableList<Bitmap>,
        iconMaxSidePx: Int
    ) {
        if (artwork == null) {
            remoteViews.setImageViewResource(
                viewId,
                if (isSelected) R.drawable.pt_star_filled else R.drawable.pt_star_outline
            )
            applyTint(remoteViews, viewId, data, isSelected)
            return
        }

        if (isSelected) {
            // A selected variant that fails falls back to this position's own artwork rather than to
            // a star: the row is otherwise intact, and swapping one position to a star mid-tap would
            // be a worse surprise than simply not changing the picture.
            val selectedUrl = data.positions.getOrNull(position - 1)?.selectedIconUrl
            val selected = selectedUrl?.let { mediaManager.getImageBitmap(it) }
            if (selectedUrl != null && selected == null) {
                PTLog.debug(
                    "${PTConstants.PT_RATING_ICON_PREFIX}$position${PTConstants.PT_RATING_ICON_SELECTED_SUFFIX} " +
                            "could not be loaded, showing the unselected artwork instead"
                )
            }
            remoteViews.setImageViewBitmap(
                viewId, scaled(selected ?: artwork, iconMaxSidePx, ownedBitmaps)
            )
            applyTint(remoteViews, viewId, data, true)
            return
        }

        // Scale first so the greyscale copy is made at the size actually drawn, not at whatever
        // the campaign uploaded. Both are derived copies, so both are ours to recycle.
        val sized = scaled(artwork, iconMaxSidePx, ownedBitmaps)
        val grey = Utils.toGreyscale(sized)
        if (grey != null) {
            remoteViews.setImageViewBitmap(viewId, grey)
            ownedBitmaps.add(grey)
        } else {
            remoteViews.setImageViewBitmap(viewId, sized)
        }
        applyTint(remoteViews, viewId, data, false)
    }

    /**
     * [bitmap] no larger than [maxSidePx], recording the scaled copy as owned so it is recycled once
     * the notification is posted.
     *
     * The cache's own bitmap is never recycled here, so identity is what decides ownership: a scaled
     * copy is a different object, an unchanged one is the cache's and must be left alone.
     */
    private fun scaled(bitmap: Bitmap, maxSidePx: Int, ownedBitmaps: MutableList<Bitmap>): Bitmap {
        if (maxSidePx <= 0) return bitmap
        val result = NotificationBitmapUtils.scaleDownTo(bitmap, maxSidePx)
        if (result !== bitmap) ownedBitmaps.add(result)
        return result
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
     *
     * The button stays in the tree from the first render (R-28 makes the no-selection tap a no-op
     * rather than hiding it, so the layout never jumps), but it is drawn dimmed until a position is
     * picked. Both routes have to dim: a payload that configures no colours keeps the layout's own
     * grey, so there the container background is the only thing left to change.
     */
    private fun renderCta(
        remoteViews: RemoteViews,
        data: CustomRatingTemplateData,
        isEnabled: Boolean
    ) {
        if (!data.hasSubmitButton) {
            // Half a submit button is almost always a mistake, and it silently changes the template
            // from deferred submit to submit-on-tap — worth naming the missing key.
            if (!data.ctaData.label.isNullOrBlank() || !data.ctaData.deepLink.isNullOrBlank()) {
                PTLog.debug(
                    "Only one of ${PTConstants.PT_RATING_CTA_LABEL} / ${PTConstants.PT_RATING_CTA_DL} " +
                            "is set; a submit button needs both, so the position tap will submit instead"
                )
            }
            // No submit button configured: the position tap submits directly (FR-DSH-09).
            remoteViews.setViewVisibility(R.id.custom_rating_cta, View.GONE)
            return
        }
        val cta = data.ctaData
        remoteViews.setViewVisibility(R.id.custom_rating_cta, View.VISIBLE)
        cta.label?.let {
            remoteViews.setTextViewText(R.id.custom_rating_cta_label, it)
            // The layout carries a static "Confirm Button" description, which a screen reader
            // announces in place of the label the campaign configured. Describe the button by the
            // text it actually shows, the same way [renderLabel] does for a text chip.
            remoteViews.setContentDescription(R.id.custom_rating_cta_label, it)
        }
        // Fall back to the notification's own title colour so there is always a concrete colour to
        // dim — the label otherwise inherits its colour from a style the RemoteViews cannot read.
        val textColor = (cta.textColor ?: data.baseContent.colorData.titleColor)
            ?.let { Utils.getColourOrNull(it) }
        if (textColor != null) {
            remoteViews.setTextColor(R.id.custom_rating_cta_label, dimIfDisabled(textColor, isEnabled))
        }

        val border = cta.borderColor?.let { Utils.getColourOrNull(it) }
            ?.let { dimIfDisabled(it, isEnabled) }
        // The radius arrives in dp but the canvas is a fixed pixel size, so scale it to keep the
        // rendered curve proportional to the button the user actually sees.
        val radiusPx = cta.cornerRadiusDp.toFloat() *
                (CTA_BITMAP_HEIGHT.toFloat() / DEFAULT_CTA_HEIGHT_DP)
        val borderWidth = if (border != null) PTConstants.PT_BTN_BORDER_WIDTH_DEFAULT else null

        val background = ctaBackground(cta, isEnabled, border, radiusPx, borderWidth)
        if (background == null) {
            // Nothing to draw: the layout's own grey stands in, so the container background is the
            // only thing left to dim.
            remoteViews.setInt(
                R.id.custom_rating_cta, "setBackgroundResource",
                if (isEnabled) R.color.customButtonBackground
                else R.color.customButtonBackgroundDisabled
            )
            return
        }
        remoteViews.setImageViewBitmap(R.id.custom_rating_cta_bg, background)
        // The generated bitmap already carries the fill, so drop the placeholder behind it or its
        // square corners show through the rounded ones.
        remoteViews.setInt(R.id.custom_rating_cta, "setBackgroundColor", Color.TRANSPARENT)
    }

    /**
     * [color] at [CTA_DISABLED_ALPHA] opacity while nothing is selected. Alpha is scaled rather than
     * replaced so an already-translucent payload colour does not become more opaque when dimmed.
     */
    private fun dimIfDisabled(color: Int, isEnabled: Boolean): Int =
        if (isEnabled) color
        else ColorUtils.setAlphaComponent(color, (Color.alpha(color) * CTA_DISABLED_ALPHA).toInt())

    /**
     * The submit button's background, or null when the payload configures no colours at all and the
     * layout's own grey should stand.
     *
     * The three styles and their fallbacks match the action buttons exactly
     * ([com.clevertap.android.pushtemplates.content.VerticalImageContentView]): a gradient with only
     * one of its two colours set is not a gradient, so it falls back to the solid fill, and a solid
     * with no fill either falls back to the layout.
     */
    private fun ctaBackground(
        cta: com.clevertap.android.pushtemplates.RatingCtaData,
        isEnabled: Boolean,
        border: Int?,
        radiusPx: Float,
        borderWidth: Float?
    ): Bitmap? {
        val color1 = cta.gradientColor1?.let { Utils.getColourOrNull(it) }
        val color2 = cta.gradientColor2?.let { Utils.getColourOrNull(it) }
        val gradient = color1 != null && color2 != null

        if (gradient && cta.style == ButtonStyle.GRADIENT_LINEAR) {
            return NotificationBitmapUtils.createLinearGradientBitmap(
                dimIfDisabled(color1!!, isEnabled), dimIfDisabled(color2!!, isEnabled),
                cta.gradientDirection, CTA_BITMAP_WIDTH, CTA_BITMAP_HEIGHT, radiusPx,
                border, borderWidth
            )
        }
        if (gradient && cta.style == ButtonStyle.GRADIENT_RADIAL) {
            return NotificationBitmapUtils.createRadialBitmap(
                dimIfDisabled(color1!!, isEnabled), dimIfDisabled(color2!!, isEnabled),
                CTA_BITMAP_WIDTH, CTA_BITMAP_HEIGHT, radiusPx, border, borderWidth
            )
        }
        if (cta.style != ButtonStyle.SOLID && !gradient) {
            PTLog.debug(
                "${PTConstants.PT_RATING_CTA_STYLE} is ${cta.style.key} but " +
                        "${PTConstants.PT_RATING_CTA_GRAD_CLR1} / ${PTConstants.PT_RATING_CTA_GRAD_CLR2} " +
                        "are not both set, falling back to the solid fill"
            )
        }

        val fill = cta.backgroundColor?.let { Utils.getColourOrNull(it) }
        if (fill == null && border == null) return null
        return NotificationBitmapUtils.createSolidBitmap(
            bgColor = dimIfDisabled(fill ?: Color.TRANSPARENT, isEnabled),
            borderColor = border,
            width = CTA_BITMAP_WIDTH,
            height = CTA_BITMAP_HEIGHT,
            cornerRadius = radiusPx,
            borderWidth = borderWidth
        )
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
