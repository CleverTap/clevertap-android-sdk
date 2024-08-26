package com.clevertap.android.sdk.inapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;

import com.clevertap.android.sdk.R;

public class CTInAppHtmlHeaderFragment extends CTInAppBasePartialHtmlFragment {

    @Override
    ViewGroup getLayout(View view) {
        return view.findViewById(R.id.inapp_html_header_frame_layout);
    }

    @Override
    View getView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.inapp_html_header, container, false);
    }

    @Override
    void fillTopBottomMargin(Insets bars, ViewGroup.MarginLayoutParams mlp) {
        mlp.topMargin = bars.top;
    }
}
