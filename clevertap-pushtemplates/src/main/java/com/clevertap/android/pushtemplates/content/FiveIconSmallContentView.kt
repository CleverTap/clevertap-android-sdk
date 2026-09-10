package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.FiveIconContentView.Companion.hasCollapsedText

internal class FiveIconSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: FiveIconsTemplateData,
    extras: Bundle
) : FiveIconContentView(
    context,
    renderer,
    if (hasCollapsedText(data)) R.layout.five_cta_collapsed_with_text else R.layout.five_cta_collapsed
) {

    init {
        setupHeader(data, renderer)
        if (hasCollapsedText(data)) {
            setupTextRow(data, hideMessage = data.iconTextData.message.isNullOrEmpty())
        } else {
            hideTextRow()
        }
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        if (fitIconRowToCollapsedView(hasText = hasCollapsedText(data))) {
            setupIcons(data)
            setupIconClicks(data, extras, renderer.notificationId)
        }
    }

    /**
     * Android 12 cut the height a collapsed custom view is given from 106dp to 48dp, but only for
     * apps that target it: one still targeting Android 11 keeps the full height on every device
     * and needs nothing done here. Note this cannot be a -v31 resource qualifier, which follows
     * the device rather than what the app targets.
     *
     * A title and message fill those 48dp on their own, so the strip is dropped from the collapsed
     * view rather than squeezed into what is left, where it came out a few dp tall; the expanded
     * view has room to show the icons at full size. An icon only campaign has no text to make room
     * for and keeps its strip, resized to the 48dp so the system does not clip it.
     *
     * @return whether the strip is shown, and so worth binding icons and click intents into. A
     * hidden strip is left empty: its bitmaps would be parcelled to the system a second time on
     * top of the expanded view's copy, and five large images is the payload shape that runs into
     * the notification size limit. The expanded view still loads all five, so the renderer's
     * fallback on too many failed images is unaffected.
     */
    private fun fitIconRowToCollapsedView(hasText: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.S
        ) {
            return true
        }
        if (hasText) {
            remoteView.setViewVisibility(R.id.five_cta_icon_row, View.GONE)
            return false
        }
        remoteView.setViewLayoutHeight(
            R.id.five_cta_icon_row,
            context.resources.getDimension(R.dimen.five_cta_icon_row_compact),
            TypedValue.COMPLEX_UNIT_PX
        )
        return true
    }
}
