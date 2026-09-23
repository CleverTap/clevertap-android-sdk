package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class IconsBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: IconsTemplateData,
    extras: Bundle
) : IconsContentView(context, renderer, R.layout.icons_expanded) {

    init {
        setupHeader(data, renderer)
        if (hasText(data)) {
            // pt_msg_summary takes the message slot when expanded.
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
