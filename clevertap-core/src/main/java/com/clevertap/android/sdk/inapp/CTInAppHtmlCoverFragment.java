package com.clevertap.android.sdk.inapp;

import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.clevertap.android.sdk.Constants;

public class CTInAppHtmlCoverFragment extends CTInAppBaseFullHtmlFragment {

    @Override
    protected RelativeLayout.LayoutParams getLayoutParamsForCloseButton() {
        RelativeLayout.LayoutParams closeIvLp = new RelativeLayout
                .LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        // Position it at the top right corner
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, webView.getId());
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_TOP, webView.getId());

        int sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 4;
        closeIvLp.setMargins(0, sub, sub, 0);
        return closeIvLp;
    }
}
