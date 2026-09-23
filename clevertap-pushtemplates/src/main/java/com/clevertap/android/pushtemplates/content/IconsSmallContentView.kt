package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.IconsContentView.Companion.hasCollapsedText

internal class IconsSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: IconsTemplateData,
    extras: Bundle
) : IconsContentView(
    context,
    renderer,
    if (hasCollapsedText(data)) R.layout.icons_collapsed_with_text else R.layout.icons_collapsed
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
     * Apps targeting Android 12+ get a 48dp collapsed view. With text, the icon row is hidden
     * (icons show when expanded); icon-only, the row is resized to 48dp. This checks the target
     * SDK, so it cannot be a -v31 resource.
     *
     * @return whether the icon row is shown. A hidden row is not bound, to keep the notification
     * payload small.
     */
    private fun fitIconRowToCollapsedView(hasText: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.S
        ) {
            return true
        }
        if (hasText) {
            remoteView.setViewVisibility(R.id.icons_icon_row, View.GONE)
            return false
        }
        remoteView.setViewLayoutHeight(
            R.id.icons_icon_row,
            context.resources.getDimension(R.dimen.icons_icon_row_compact),
            TypedValue.COMPLEX_UNIT_PX
        )
        return true
    }
}
