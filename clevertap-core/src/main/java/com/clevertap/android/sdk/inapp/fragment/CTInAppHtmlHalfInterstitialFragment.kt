package com.clevertap.android.sdk.inapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment

internal class CTInAppHtmlHalfInterstitialFragment : CTInAppBaseFullHtmlFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inAppView = super.onCreateView(inflater, container, savedInstanceState)
        inAppView?.applyInsetsWithMarginAdjustment { insets, mlp ->
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            mlp.topMargin = insets.top
            mlp.bottomMargin = insets.bottom
        }
        return inAppView
    }
}
