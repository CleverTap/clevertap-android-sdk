package com.clevertap.android.sdk.inapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment

internal class CTInAppHtmlCoverFragment : CTInAppBaseFullHtmlFragment() {

    override fun getLayoutParamsForCloseButton(webViewId: Int): LayoutParams {
        val closeIvLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        // Position it at the top right corner
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_TOP)

        val sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 4
        closeIvLp.setMargins(0, sub, sub, 0)
        return closeIvLp
    }

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
