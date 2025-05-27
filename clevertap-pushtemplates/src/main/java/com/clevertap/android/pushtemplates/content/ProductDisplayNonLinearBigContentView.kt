package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer

internal class ProductDisplayNonLinearBigContentView(context: Context, renderer: TemplateRenderer, extras: Bundle) :
    ProductDisplayLinearBigContentView(context, renderer, extras, R.layout.product_display_template) {

    init {
        setCustomContentViewTitle(productName)
        setCustomContentViewMessage(productMessage)
        setCustomTextColour(renderer.pt_msg_clr, R.id.msg)
        setCustomTextColour(renderer.pt_title_clr, R.id.title)
    }
}