package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

/**
 * Collapsed view with icons only. Text shows when expanded.
 */
internal class IconsSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: IconsTemplateData,
    extras: Bundle
) : IconsContentView(context, renderer, R.layout.icons_collapsed) {

    init {
        setupHeader(data, renderer)
        hideTextRow()
        setCustomBackgroundColour(data.baseContent.colorData.backgroundColor, R.id.content_view_big)
        fitIconRowToCollapsedView()
        setupIcons(data)
        setupIconClicks(data, extras, renderer.notificationId)
    }

    /**
     * Apps targeting Android 12+ get a 48dp collapsed view. Uses target SDK, so not a -v31 resource.
     */
    private fun fitIconRowToCollapsedView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.S
        ) {
            return
        }
        remoteView.setViewLayoutHeight(
            R.id.icons_icon_row,
            context.resources.getDimension(R.dimen.icons_icon_row_compact),
            TypedValue.COMPLEX_UNIT_PX
        )
    }
}
