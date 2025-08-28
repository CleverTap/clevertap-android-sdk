package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.ProductTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class ProductDisplayNonLinearBigContentView(
    context: Context,
    renderer: TemplateRenderer,
    data: ProductTemplateData,
    extras: Bundle,
) :
    ProductDisplayLinearBigContentView(
        context,
        renderer,
        data,
        extras,
        R.layout.product_display_template
    ) {

    init {
        setCustomContentViewTitle(productName)
        setCustomContentViewMessage(productMessage)
        setCustomTextColour(data.baseContent.colorData.messageColor, R.id.msg)
        setCustomTextColour(data.baseContent.colorData.titleColor, R.id.title)
    }
}