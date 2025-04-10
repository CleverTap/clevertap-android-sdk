package com.clevertap.android.sdk.inapp;

import android.webkit.WebView;
import android.webkit.WebViewClient;

class InAppWebViewClient extends WebViewClient {

    private final CTInAppBaseFragment ctInAppBaseFullHtmlFragment;

    InAppWebViewClient(CTInAppBaseFragment ctInAppBaseFullHtmlFragment) {
        super();
        this.ctInAppBaseFullHtmlFragment = ctInAppBaseFullHtmlFragment;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        ctInAppBaseFullHtmlFragment.openActionUrl(url);
        return true;
    }
}
