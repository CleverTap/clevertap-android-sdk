package com.clevertap.android.sdk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CTInAppHtmlHeaderFragment extends CTInAppBasePartialHtmlFragment {

    @Override
    ViewGroup getLayout(View view) {
        return view.findViewById(R.id.inapp_html_header_frame_layout);
    }

    @Override
    View getView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.inapp_html_header, container, false);
    }

}
