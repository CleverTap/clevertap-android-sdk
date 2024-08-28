package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.CTXtensions.applySystemBarsInsetsWithMargin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = super.onCreateView(inflater, container, savedInstanceState);
        if (inAppView != null) {
            applySystemBarsInsetsWithMargin(inAppView, (insets, mlp) -> {
                mlp.leftMargin = insets.left;
                mlp.rightMargin = insets.right;
                mlp.topMargin = insets.top;
                mlp.bottomMargin = insets.bottom;
                return null;
            });
        }
        return inAppView;
    }
}
