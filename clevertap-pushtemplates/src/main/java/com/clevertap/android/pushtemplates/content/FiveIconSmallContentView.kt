package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory

internal class FiveIconSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : ContentView(context,
    if (hasText(data)) R.layout.five_cta_collapsed_with_text else R.layout.five_cta_collapsed,
    renderer.templateMediaManager) {

    private var imageCounter: Int = 0

    init {
        /**
         * five_cta_collapsed is icon-only and carries none of the text view ids, so the text
         * keys are only applied when the text variant of the layout is in use.
         */
        if (hasText(data)) {
            setCustomContentViewBasicKeys(
                data.baseContent.textData.subtitle,
                data.baseContent.colorData.metaColor
            )
            setCustomContentViewTitle(data.iconTextData.title)
            setCustomContentViewMessage(data.iconTextData.message)
            setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
            setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)

        val ctaIds = listOf(R.id.cta1, R.id.cta2, R.id.cta3, R.id.cta4, R.id.cta5)
        val fallbackDescriptions = listOf(
            R.string.pt_five_icon_1,
            R.string.pt_five_icon_2,
            R.string.pt_five_icon_3,
            R.string.pt_five_icon_4,
            R.string.pt_five_icon_5
        )
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


        extras.putInt(PTConstants.PT_NOTIF_ID, renderer.notificationId)
        extras.putBoolean(Constants.CLOSE_SYSTEM_DIALOGS, true)

        val deepLinkList = data.baseContent.deepLinkList

        // Validation guarantees at least three deep links; icons 4 and 5 are wired only when
        // their deep link is present, matching the icons made visible above.
        deepLinkList.take(ctaIds.size).forEachIndexed { index, deepLink ->
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

        if (imageCounter > 2) {
            PTLog.debug("More than 2 images were not retrieved in 5CTA Notification, not displaying Notification.")
        }
    }

    /**
     * Returns total number of five icon URL's which does not convert to bitmap
     */
    internal fun getUnloadedFiveIconsCount(): Int {
        return imageCounter
    }

    companion object {

        /**
         * Only pt_title/pt_msg drive the layout choice. nt/nm are populated on every campaign,
         * so consulting them here would make the icon-only layout unreachable.
         */
        internal fun hasText(data: FiveIconsTemplateData): Boolean {
            return !data.iconTextData.title.isNullOrEmpty() ||
                    !data.iconTextData.message.isNullOrEmpty()
        }
    }
}
