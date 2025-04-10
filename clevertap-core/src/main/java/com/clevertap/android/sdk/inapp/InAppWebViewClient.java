package com.clevertap.android.sdk.inapp;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

class InAppWebViewClient extends WebViewClient {

    private final WeakReference<CTInAppBaseFragment> ctInAppBaseFullHtmlFragment;

    InAppWebViewClient(CTInAppBaseFragment ctInAppBaseFullHtmlFragment) {
        super();
        this.ctInAppBaseFullHtmlFragment = new WeakReference<>(ctInAppBaseFullHtmlFragment);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (ctInAppBaseFullHtmlFragment != null && ctInAppBaseFullHtmlFragment.get() != null) {
            ctInAppBaseFullHtmlFragment.get().openActionUrl(url);
        }
        return true;
    }
}
