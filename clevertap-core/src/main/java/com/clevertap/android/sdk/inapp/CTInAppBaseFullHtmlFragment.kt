package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import com.clevertap.android.sdk.CTWebInterface
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView

internal abstract class CTInAppBaseFullHtmlFragment : CTInAppBaseFullFragment() {

    protected var webView: CTInAppWebView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return displayHTMLView(inflater, container)
    }

    override fun onDestroyView() {
        cleanupWebView()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reDrawInApp()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reDrawInApp()
    }

    protected open fun getLayoutParamsForCloseButton(webViewId: Int): RelativeLayout.LayoutParams {
        val closeIvLp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
        )
        // Position it at the top right corner
        closeIvLp.addRule(RelativeLayout.ABOVE, webViewId)
        closeIvLp.addRule(RelativeLayout.RIGHT_OF, webViewId)

        val sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 2
        closeIvLp.setMargins(-sub, 0, 0, -sub)
        return closeIvLp
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun displayHTMLView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val inAppView: View
        try {
            inAppView = inflater.inflate(R.layout.inapp_html_full, container, false)
            val rl = inAppView.findViewById<RelativeLayout>(R.id.inapp_html_full_relative_layout)
            val webViewLp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
            )
            webViewLp.addRule(RelativeLayout.CENTER_IN_PARENT)

            initWebViewLayoutParams(webViewLp)
            val webView = CTInAppWebView(
                requireContext(),
                inAppNotification.width,
                inAppNotification.height,
                inAppNotification.widthPercentage,
                inAppNotification.heightPercentage
            )
            this.webView = webView
            val webViewClient = InAppWebViewClient(this)
            webView.setWebViewClient(webViewClient)

            if (inAppNotification.isJsEnabled) {
                val instance = CleverTapAPI.instanceWithConfig(activity, config)
                val ctWebInterface = CTWebInterface(instance, this)
                webView.setJavaScriptInterface(ctWebInterface)
            }

            if (isDarkenEnabled()) {
                rl.background = ColorDrawable(-0x45000000)
            } else {
                rl.background = ColorDrawable(0x00000000)
            }

            rl.addView(webView, webViewLp)

            if (isCloseButtonEnabled()) {
                closeImageView = CloseImageView(requireContext())
                val closeIvLp = getLayoutParamsForCloseButton(webView.id)

                closeImageView!!.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        didDismiss(null)
                    }
                })
                rl.addView(closeImageView, closeIvLp)
            }
        } catch (t: Throwable) {
            config.getLogger().verbose(config.accountId, "Fragment view not created", t)
            return null
        }
        return inAppView
    }

    private fun initWebViewLayoutParams(params: RelativeLayout.LayoutParams) {
        val pos = inAppNotification.position
        when (pos) {
            Constants.INAPP_POSITION_TOP -> params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            Constants.INAPP_POSITION_LEFT -> params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            Constants.INAPP_POSITION_BOTTOM -> params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            Constants.INAPP_POSITION_RIGHT -> params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            Constants.INAPP_POSITION_CENTER -> params.addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        params.setMargins(0, 0, 0, 0)
    }

    private fun isCloseButtonEnabled(): Boolean {
        return inAppNotification.isShowClose
    }

    private fun isDarkenEnabled(): Boolean {
        return inAppNotification.isDarkenScreen
    }

    private fun reDrawInApp() {
        val webView = this.webView ?: return
        webView.updateDimension()

        if (inAppNotification.customInAppUrl.isEmpty()) {
            var mHeight = webView.dim.y
            var mWidth = webView.dim.x

            val d = resources.displayMetrics.density
            mHeight = (mHeight / d).toInt()
            mWidth = (mWidth / d).toInt()

            var html = inAppNotification.html

            val style =
                "<style>body{width: ${mWidth}px; height: ${mHeight}px; margin: 0; padding:0;}</style>"
            html = html.replaceFirst("<head>".toRegex(), "<head>$style")
            Logger.v("Density appears to be $d")

            webView.setInitialScale((d * 100).toInt())
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } else {
            val url = inAppNotification.customInAppUrl
            webView.setWebViewClient(WebViewClient())
            webView.loadUrl(url)
        }
    }

    private fun cleanupWebView() {
        try {
            webView?.cleanup(inAppNotification.isJsEnabled)
            webView = null
        } catch (e: Exception) {
            config.getLogger().verbose("cleanupWebView -> there was a crash in cleanup", e)
            //no-op; we are anyway destroying everything. This is just for safety.
        }
    }
}
