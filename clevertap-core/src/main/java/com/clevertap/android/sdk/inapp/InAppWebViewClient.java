package com.clevertap.android.sdk.inapp;

import android.webkit.WebView;
import android.webkit.WebViewClient;

class InAppWebViewClient extends WebViewClient {

    private final CTInAppBaseFullHtmlFragment ctInAppBaseFullHtmlFragment;

    InAppWebViewClient(CTInAppBaseFullHtmlFragment ctInAppBaseFullHtmlFragment) {
        super();
        this.ctInAppBaseFullHtmlFragment = ctInAppBaseFullHtmlFragment;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        ctInAppBaseFullHtmlFragment.openActionUrl(url);
        return true;
    }
}
