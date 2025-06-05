package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.CTXtensions.applyInsetsWithMarginAdjustment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CTInAppHtmlInterstitialFragment extends CTInAppBaseFullHtmlFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = super.onCreateView(inflater, container, savedInstanceState);
        if (!isFullscreen && inAppView != null) {
            applyInsetsWithMarginAdjustment(inAppView, (insets, mlp) -> {
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
