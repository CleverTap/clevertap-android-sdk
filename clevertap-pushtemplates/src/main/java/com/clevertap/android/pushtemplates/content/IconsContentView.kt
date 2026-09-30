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

    /**
     * Number of icon images that failed to load.
     */
    internal var unloadedIconsCount: Int = 0
        private set

    /**
     * App name, time and subtitle. Kept for icon-only campaigns: below Android 12 it names the app.
     */
    protected fun setupHeader(data: IconsTemplateData, renderer: TemplateRenderer) {
        setCustomContentViewBasicKeys(
            data.baseContent.textData.subtitle,
            data.baseContent.colorData.metaColor
        )
        setCustomContentViewSmallIcon(renderer.smallIconBitmap, renderer.smallIcon)
    }

    /**
     * Binds pt_title/pt_msg and hides whichever is missing.
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
                unloadedIconsCount++
            }
        }
    }

    /**
     * One click intent per deep link. The app dismisses on tap, not the SDK. pt_dismiss_on_click is
     * removed because the documented handler reads its presence as "keep".
     */
    protected fun setupIconClicks(data: IconsTemplateData, extras: Bundle, notificationId: Int) {
        extras.putInt(PTConstants.PT_NOTIF_ID, notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)
        val autoCancel = !extras.getString(PTConstants.PT_DISMISS_ON_CLICK).equals("false", ignoreCase = true)

        data.baseContent.deepLinkList.take(ctaIds.size).forEachIndexed { index, deepLink ->
            val ctaNumber = index + 1
            val bundleCTA = extras.clone() as Bundle
            bundleCTA.remove(PTConstants.PT_DISMISS_ON_CLICK)
            bundleCTA.putBoolean("cta$ctaNumber", true)
            bundleCTA.putString(Constants.DEEP_LINK_KEY, deepLink)
            bundleCTA.putString(Constants.KEY_C2A, PTConstants.PT_5CTA_C2A_KEY + ctaNumber + "_" + deepLink)
            bundleCTA.putString(PTConstants.PT_ACTION_ID, "cta$ctaNumber")
            bundleCTA.putBoolean(PTConstants.PT_AUTO_CANCEL, autoCancel)
            remoteView.setOnClickPendingIntent(
                ctaIds[index],
                LaunchPendingIntentFactory.getLaunchPendingIntent(bundleCTA, context)
            )
        }
    }

    companion object {

        private val ctaIds = listOf(R.id.cta1, R.id.cta2, R.id.cta3, R.id.cta4, R.id.cta5)
        // Reuses the Five Icons strings and 5cta_ click prefix.
        private val fallbackDescriptions = listOf(
            R.string.pt_five_icon_1,
            R.string.pt_five_icon_2,
            R.string.pt_five_icon_3,
            R.string.pt_five_icon_4,
            R.string.pt_five_icon_5
        )

        /**
         * Uses pt_* keys only. nt/nm are always set, so they would hide the icon-only layout.
         */
        internal fun hasText(data: IconsTemplateData): Boolean {
            return !data.iconTextData.title.isNullOrEmpty() ||
                    !data.iconTextData.message.isNullOrEmpty() ||
                    !data.iconTextData.messageSummary.isNullOrEmpty()
        }
    }
}
