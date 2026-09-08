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

internal class FiveIconBigContentView constructor(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : ContentView(context, R.layout.five_cta_expanded, renderer.templateMediaManager) {

    private var imageCounter: Int = 0

    init {
        /**
         * five_cta_expanded always includes the text row, so it is hidden outright when the
         * campaign set no pt_title/pt_msg - otherwise the icons sit below an empty strip.
         */
        if (FiveIconSmallContentView.hasText(data)) {
            setCustomContentViewBasicKeys(
                data.baseContent.textData.subtitle,
                data.baseContent.colorData.metaColor
            )
            setCustomContentViewTitle(data.iconTextData.title)
            setCustomContentViewMessage(data.iconTextData.message)
            // Either key may be set on its own; hide the other view so it does not leave a blank line.
            if (data.iconTextData.title.isNullOrEmpty()) remoteView.setViewVisibility(R.id.title, View.GONE)
            // The expanded view shows pt_msg_summary in the message slot, so keep it when only the summary is set.
            if (data.iconTextData.message.isNullOrEmpty() && data.baseContent.textData.messageSummary.isNullOrEmpty()) {
                remoteView.setViewVisibility(R.id.msg, View.GONE)
            }
            // The text row reserves a 36dp large icon slot; hide it unless pt_ico is set, otherwise
            // a single text line sits in a row that is still icon-height tall.
            setCustomContentViewLargeIcon(data.baseContent.iconData.largeIcon)
            setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
            setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
            setCustomContentViewMessageSummary(data.baseContent.textData.messageSummary)
        } else {
            remoteView.setViewVisibility(R.id.rel_lyt, View.GONE)
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
}
