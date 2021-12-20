package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

class ProductDisplayNonLinearBigContentView(context: Context, renderer: TemplateRenderer, extras: Bundle) :
    ProductDisplayLinearBigContentView(context, renderer, extras, R.layout.product_display_template) {

    init {
        setCustomContentViewTitle(productName)
        setCustomContentViewMessage(productMessage)
        setCustomContentViewElementColour(R.id.msg, renderer.pt_msg_clr)
        setCustomContentViewElementColour(R.id.title, renderer.pt_title_clr)
    }

    private fun setCustomContentViewElementColour(rId: Int, colour: String?) {
        if (colour != null && colour.isNotEmpty()) {
            remoteView.setTextColor(rId, Utils.getColour(colour, PTConstants.PT_COLOUR_BLACK))
        }
    }
}