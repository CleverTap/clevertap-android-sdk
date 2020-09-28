package com.clevertap.demo

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CTWebInterface
import com.clevertap.android.sdk.CleverTapAPI

class WebViewActivity : AppCompatActivity() {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview)
        findViewById<WebView>(R.id.webview)?.apply {
            settings.javaScriptEnabled = true
            loadUrl("file:///android_asset/sampleHTMLCode.html")
            settings.allowContentAccess = false
            settings.allowFileAccess = false
            settings.allowFileAccessFromFileURLs = false
            addJavascriptInterface(CTWebInterface(CleverTapAPI.getDefaultInstance(this@WebViewActivity)), "CleverTap")
        }
    }
}