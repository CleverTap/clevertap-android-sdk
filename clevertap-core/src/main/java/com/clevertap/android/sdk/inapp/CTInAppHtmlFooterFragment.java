package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.CTXtensions.applyInsetsWithMarginAdjustment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.clevertap.android.sdk.R;

public class CTInAppHtmlFooterFragment extends CTInAppBasePartialHtmlFragment {

    @Override
    ViewGroup getLayout(View view) {
        return view.findViewById(R.id.inapp_html_footer_frame_layout);
    }

    @Override
    View getView(LayoutInflater inflater, ViewGroup container) {
        View inAppView =  inflater.inflate(R.layout.inapp_html_footer, container, false);
        applyInsetsWithMarginAdjustment(inAppView, (insets, mlp) -> {
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;
            mlp.bottomMargin = insets.bottom;
            return null;
        });
        return inAppView;
    }
}
