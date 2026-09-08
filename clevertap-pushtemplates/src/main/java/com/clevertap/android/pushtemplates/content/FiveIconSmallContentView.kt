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
) : FiveIconContentView(
    context,
    renderer,
    if (hasText(data)) R.layout.five_cta_collapsed_with_text else R.layout.five_cta_collapsed
) {

    init {
        /**
         * five_cta_collapsed is icon-only and carries none of the text view ids, so the text
         * keys are only applied when the text variant of the layout is in use.
         */
        if (hasText(data)) {
            setupTextRow(data, hideMessage = data.iconTextData.message.isNullOrEmpty())
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        setupIcons(data)
        setupIconClicks(data, extras, renderer.notificationId)
    }
}
