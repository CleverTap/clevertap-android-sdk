package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import java.lang.ref.WeakReference

internal class InAppWebViewClient(fragment: CTInAppBaseFragment) : WebViewClient() {
    private val fragmentWr = WeakReference(fragment)

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        fragmentWr.get()?.openActionUrl(url) ?: Logger.v("InAppWebViewClient : Android view is gone, not opening url")
        return true
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        request.url?.toString()?.let { url ->
            fragmentWr.get()?.openActionUrl(url) ?: Logger.v("InAppWebViewClient : Android view is gone, not opening url")
        } ?: Logger.v("InAppWebViewClient : Url to open is null; not processing")
        return true
    }
}
