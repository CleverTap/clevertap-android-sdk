package com.clevertap.android.sdk.inapp.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment

internal class CTInAppHtmlFooterFragment : CTInAppBasePartialHtmlFragment() {

    override fun getLayout(view: View?): ViewGroup? {
        return view?.findViewById(R.id.inapp_html_footer_frame_layout)
    }

    override fun getView(inflater: LayoutInflater, container: ViewGroup?): View {
        val inAppView = inflater.inflate(R.layout.inapp_html_footer, container, false)
        inAppView.applyInsetsWithMarginAdjustment { insets, mlp ->
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            mlp.bottomMargin = insets.bottom
        }
        return inAppView
    }
}
