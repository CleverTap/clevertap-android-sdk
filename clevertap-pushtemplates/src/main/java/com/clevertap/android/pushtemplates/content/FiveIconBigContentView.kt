package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class FiveIconBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : FiveIconContentView(context, renderer, R.layout.five_cta_expanded) {

    init {
        /**
         * five_cta_expanded always includes the text row, so it is hidden outright when the
         * campaign set no pt_title/pt_msg - otherwise the icons sit below an empty strip.
         */
        if (hasText(data)) {
            // The expanded view shows pt_msg_summary in the message slot, so keep it when only the summary is set.
            val messageSummary = data.baseContent.textData.messageSummary
            setupTextRow(
                data,
                hideMessage = data.iconTextData.message.isNullOrEmpty() && messageSummary.isNullOrEmpty()
            )
            setCustomContentViewMessageSummary(messageSummary)
        } else {
            remoteView.setViewVisibility(R.id.rel_lyt, View.GONE)
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setupIcons(data)
        setupIconClicks(data, extras, renderer.notificationId)
    }
}
