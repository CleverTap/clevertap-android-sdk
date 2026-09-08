package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
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
        setupHeader(data, renderer)
        if (hasExpandedText(data)) {
            // The expanded view shows pt_msg_summary in the message slot, so keep it when only the summary is set.
            val messageSummary = data.iconTextData.messageSummary
            setupTextRow(
                data,
                hideMessage = data.iconTextData.message.isNullOrEmpty() && messageSummary.isNullOrEmpty()
            )
            setCustomContentViewMessageSummary(messageSummary)
        } else {
            hideTextRow()
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setupIcons(data)
        setupIconClicks(data, extras, renderer.notificationId)
    }
}
