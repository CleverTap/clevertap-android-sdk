package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.isNotNullAndEmpty

internal class ProductDisplayNonLinearBigContentView(context: Context, renderer: TemplateRenderer, extras: Bundle) :
    ProductDisplayLinearBigContentView(context, renderer, extras, R.layout.product_display_template) {

    init {
        setCustomContentViewTitle(productName)
        setCustomContentViewMessage(productMessage)
        setCustomContentViewElementColour(R.id.msg, renderer.pt_msg_clr)
        setCustomContentViewElementColour(R.id.title, renderer.pt_title_clr)
    }

    private fun setCustomContentViewElementColour(rId: Int, colour: String?) {
        colour?.takeIf { it.isNotEmpty() }?.let {
            Utils.getColourOrNull(it)?.let { color ->
                remoteView.setTextColor(rId, color)
            }
        }
    }
}