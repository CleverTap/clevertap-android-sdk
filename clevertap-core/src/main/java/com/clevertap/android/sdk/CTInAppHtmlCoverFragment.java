package com.clevertap.android.sdk;

import android.widget.RelativeLayout;

public class CTInAppHtmlCoverFragment extends CTInAppBaseFullHtmlFragment {

    @Override
    protected RelativeLayout.LayoutParams getLayoutParamsForCloseButton() {
        RelativeLayout.LayoutParams closeIvLp = new RelativeLayout
                .LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        // Position it at the top right corner
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, webView.getId());
        closeIvLp.addRule(RelativeLayout.ALIGN_PARENT_TOP, webView.getId());

        int sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 4;
        closeIvLp.setMargins(0, sub, sub, 0);
        return closeIvLp;
    }
}
