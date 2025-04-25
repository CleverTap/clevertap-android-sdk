package com.clevertap.android.sdk.inapp

import android.webkit.WebView
import android.webkit.WebViewClient
import com.clevertap.android.sdk.Logger
import java.lang.ref.WeakReference

internal class InAppWebViewClient(fragment: CTInAppBaseFragment) : WebViewClient() {
    private val fragmentWr = WeakReference(fragment)

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        fragmentWr.get()?.openActionUrl(url) ?: Logger.v("Android view is gone, not opening url")
        return true
    }
}
