package com.clevertap.android.sdk.inapp;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

class InAppWebViewClient extends WebViewClient {

    private final WeakReference<CTInAppBaseFragment> fragmentWr;

    InAppWebViewClient(CTInAppBaseFragment ctInAppBaseFullHtmlFragment) {
        super();
        this.fragmentWr = new WeakReference<>(ctInAppBaseFullHtmlFragment);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (fragmentWr != null && fragmentWr.get() != null) {
            fragmentWr.get().openActionUrl(url);
        }
        return true;
    }
}
