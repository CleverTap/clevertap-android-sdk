package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

/**
 * Shared rendering for the five icons collapsed and expanded views: the optional text row, the
 * icon row and the per-icon click intents. Subclasses pick the layout and call the helpers.
 */
internal abstract class FiveIconContentView(
    context: Context,
    renderer: TemplateRenderer,
    layoutId: Int
) : ContentView(context, layoutId, renderer.templateMediaManager) {

    private var imageCounter: Int = 0

    /**
     * App name, timestamp and subtitle. Below Android 12 the system draws no header around a
     * custom notification, so this row is what identifies the app; it is set even for icon-only
     * campaigns. The layout-v31 variants carry no header of their own and let the system decorate.
     */
    protected fun setupHeader(data: FiveIconsTemplateData, renderer: TemplateRenderer) {
        setCustomContentViewBasicKeys(
            data.baseContent.textData.subtitle,
            data.baseContent.colorData.metaColor
        )
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
    }

    /**
     * Binds pt_title/pt_msg into the text block. Either text key may be set on its own, so the
     * unused view is hidden rather than left as a blank line. The block also reserves a 36dp
     * large icon slot, which is hidden unless pt_ico is set.
     *
     * @param hideMessage whether the message view has nothing to show; the expanded view also
     * fills this slot with pt_msg_summary, so it decides this differently from the collapsed view.
     */
    protected fun setupTextRow(data: FiveIconsTemplateData, hideMessage: Boolean) {
        setCustomContentViewTitle(data.iconTextData.title)
        setCustomContentViewMessage(data.iconTextData.message)
        if (data.iconTextData.title.isNullOrEmpty()) remoteView.setViewVisibility(R.id.title, View.GONE)
        if (hideMessage) remoteView.setViewVisibility(R.id.msg, View.GONE)
        setCustomContentViewLargeIcon(data.baseContent.iconData.largeIcon)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
    }

    /**
     * Hides the title/message block (R.id.rel_lyt inside the included text row) for icon-only
     * campaigns. The header row is a sibling and stays visible.
     */
    protected fun hideTextRow() {
        remoteView.setViewVisibility(R.id.rel_lyt, View.GONE)
    }

    /**
     * Shows one icon per image and loads it; an icon whose image cannot be loaded is hidden and
     * counted so the renderer can fall back to the basic template when too many are missing.
     */
    protected fun setupIcons(data: FiveIconsTemplateData) {
        data.imageList.forEachIndexed { index, imageData ->
            val imageUrl = imageData.url
            val altText = imageData.altText
            if (index >= ctaIds.size) return@forEachIndexed

            val viewId = ctaIds[index]
            remoteView.setViewVisibility(viewId, View.VISIBLE)

            val description = if (altText.isNotEmpty()) altText
                              else context.getString(fallbackDescriptions[index])
            remoteView.setContentDescription(viewId, description)

            val fallback = loadImageURLIntoRemoteView(
                viewId,
                imageUrl,
                remoteView,
                altText
            )

            if (fallback) {
                remoteView.setViewVisibility(viewId, View.GONE)
                imageCounter++
            }
        }
    }

    /**
     * Attaches one click intent per deep link. Validation guarantees at least three deep links,
     * and icons 4 and 5 are wired only when theirs is present.
     *
     * Note the icons shown come from pt_img and the clicks from pt_dl, which the validator checks
     * independently, so a payload with more images than deep links leaves its last icons falling
     * through to the notification's own content intent.
     */
    protected fun setupIconClicks(data: FiveIconsTemplateData, extras: Bundle, notificationId: Int) {
        extras.putInt(PTConstants.PT_NOTIF_ID, notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)

        data.baseContent.deepLinkList.take(ctaIds.size).forEachIndexed { index, deepLink ->
            val ctaNumber = index + 1
            val bundleCTA = extras.clone() as Bundle
            bundleCTA.putBoolean("cta$ctaNumber", true)
            bundleCTA.putString(Constants.DEEP_LINK_KEY, deepLink)
            bundleCTA.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + ctaNumber + "_" + deepLink)
            // Same keys the core SDK puts on action button clicks, so the documented Android 12+
            // client-side dismiss handling covers icon taps too.
            bundleCTA.putString(PTConstants.PT_ACTION_ID, "cta$ctaNumber")
            bundleCTA.putBoolean(PTConstants.PT_AUTO_CANCEL, true)
            remoteView.setOnClickPendingIntent(
                ctaIds[index],
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA, context)
            )
        }
    }

    /**
     * Returns total number of five icon URL's which does not convert to bitmap
     */
    internal fun getUnloadedFiveIconsCount(): Int {
        return imageCounter
    }

    companion object {

        private val ctaIds = listOf(R.id.cta1, R.id.cta2, R.id.cta3, R.id.cta4, R.id.cta5)
        private val fallbackDescriptions = listOf(
            R.string.pt_five_icon_1,
            R.string.pt_five_icon_2,
            R.string.pt_five_icon_3,
            R.string.pt_five_icon_4,
            R.string.pt_five_icon_5
        )

        /**
         * Only the pt_* text keys drive the layout choice. nt/nm are populated on every campaign,
         * so consulting them here would make the icon-only layout unreachable.
         *
         * The collapsed view shows pt_title/pt_msg; the expanded view shows pt_msg_summary in the
         * message slot as well, so a summary-only campaign still gets its text row there.
         */
        internal fun hasCollapsedText(data: FiveIconsTemplateData): Boolean {
            return !data.iconTextData.title.isNullOrEmpty() ||
                    !data.iconTextData.message.isNullOrEmpty()
        }

        internal fun hasExpandedText(data: FiveIconsTemplateData): Boolean {
            return hasCollapsedText(data) || !data.iconTextData.messageSummary.isNullOrEmpty()
        }
    }
}
