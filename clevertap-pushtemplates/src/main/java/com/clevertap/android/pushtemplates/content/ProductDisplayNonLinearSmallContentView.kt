package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.ProductTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class ProductDisplayNonLinearSmallContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ProductTemplateData
) : SmallContentView(
        context,
        renderer,
        data.baseContent,
        R.layout.content_view_small_single_line_msg,
    )