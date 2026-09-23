package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

/**
 * Shared rendering for the icons template collapsed and expanded views.
 */
internal abstract class IconsContentView(
    context: Context,
    renderer: TemplateRenderer,
    layoutId: Int
) : ContentView(context, layoutId, renderer.templateMediaManager) {

    private var imageCounter: Int = 0

    /**
     * App name, timestamp and subtitle. Set even for icon-only campaigns, since below Android 12
     * this row is the only thing that identifies the app.
     */
    protected fun setupHeader(data: IconsTemplateData, renderer: TemplateRenderer) {
        setCustomContentViewBasicKeys(
            data.baseContent.textData.subtitle,
            data.baseContent.colorData.metaColor
        )
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
    }

    /**
     * Binds pt_title/pt_msg, hiding whichever is missing so it does not leave a blank line.
     *
     * @param hideMessage whether the message view has nothing to show
     */
    protected fun setupTextRow(data: IconsTemplateData, hideMessage: Boolean) {
        setCustomContentViewTitle(data.iconTextData.title)
        setCustomContentViewMessage(data.iconTextData.message)
        if (data.iconTextData.title.isNullOrEmpty()) remoteView.setViewVisibility(R.id.title, View.GONE)
        if (hideMessage) remoteView.setViewVisibility(R.id.msg, View.GONE)
        setCustomContentViewLargeIcon(data.baseContent.iconData.largeIcon)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
    }

    /**
     * Hides the title/message block for icon-only campaigns. The header stays visible.
     */
    protected fun hideTextRow() {
        remoteView.setViewVisibility(R.id.rel_lyt, View.GONE)
    }

    /**
     * Loads one icon per image. Icons that fail to load are hidden and counted for the basic fallback.
     */
    protected fun setupIcons(data: IconsTemplateData) {
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
     * Attaches one click intent per deep link.
     */
    protected fun setupIconClicks(data: IconsTemplateData, extras: Bundle, notificationId: Int) {
        extras.putInt(PTConstants.PT_NOTIF_ID, notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)

        data.baseContent.deepLinkList.take(ctaIds.size).forEachIndexed { index, deepLink ->
            val ctaNumber = index + 1
            val bundleCTA = extras.clone() as Bundle
            bundleCTA.putBoolean("cta$ctaNumber", true)
            bundleCTA.putString(Constants.DEEP_LINK_KEY, deepLink)
            bundleCTA.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + ctaNumber + "_" + deepLink)
            // Same keys as core action buttons, so the app's dismiss handling covers icon taps too.
            bundleCTA.putString(PTConstants.PT_ACTION_ID, "cta$ctaNumber")
            bundleCTA.putBoolean(PTConstants.PT_AUTO_CANCEL, true)
            remoteView.setOnClickPendingIntent(
                ctaIds[index],
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA, context)
            )
        }
    }

    /**
     * Returns the number of icon images that failed to load
     */
    internal fun getUnloadedIconsCount(): Int {
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
         * Only pt_* keys decide whether the expanded view has a text row. nt/nm are always set, so
         * using them would make the icon-only layout unreachable.
         */
        internal fun hasText(data: IconsTemplateData): Boolean {
            return !data.iconTextData.title.isNullOrEmpty() ||
                    !data.iconTextData.message.isNullOrEmpty() ||
                    !data.iconTextData.messageSummary.isNullOrEmpty()
        }
    }
}
