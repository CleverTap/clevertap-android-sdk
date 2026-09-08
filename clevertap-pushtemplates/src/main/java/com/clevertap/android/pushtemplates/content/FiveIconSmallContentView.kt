package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class FiveIconSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : FiveIconContentView(context, renderer, R.layout.five_cta_collapsed_with_text) {

    init {
        setupHeader(data, renderer)
        if (hasCollapsedText(data)) {
            setupTextRow(data, hideMessage = data.iconTextData.message.isNullOrEmpty())
        } else {
            hideTextRow()
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setupIcons(data)
        setupIconClicks(data, extras, renderer.notificationId)
    }
}
