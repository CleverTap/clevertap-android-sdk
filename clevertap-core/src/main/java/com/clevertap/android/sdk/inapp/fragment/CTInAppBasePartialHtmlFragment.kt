package com.clevertap.android.sdk.inapp.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.inapp.CTInAppWebView
import com.clevertap.android.sdk.inapp.PartialHtmlInAppGestureListener

internal abstract class CTInAppBasePartialHtmlFragment : CTInAppBasePartialFragment(),
    OnTouchListener,
    OnLongClickListener {

    private lateinit var gd: GestureDetector
    private lateinit var gestureListener: PartialHtmlInAppGestureListener

    private var webView: CTInAppWebView? = null

    abstract fun getLayout(view: View?): ViewGroup?

    abstract fun getView(inflater: LayoutInflater, container: ViewGroup?): View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gestureListener = PartialHtmlInAppGestureListener(inAppHost)
        gd = GestureDetector(context, gestureListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

    override fun onLongClick(v: View?): Boolean {
        return true
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return gd.onTouchEvent(event) || (event.action == MotionEvent.ACTION_MOVE)
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun displayHTMLView(inflater: LayoutInflater, container: ViewGroup?): View? {
        try {
            val inAppView = getView(inflater, container)
            val layout = getLayout(inAppView)
            val webView = CTInAppWebView(
                inflater.context,
                inAppNotification.width,
                inAppNotification.height,
                inAppNotification.widthPercentage,
                inAppNotification.heightPercentage,
                inAppNotification.aspectRatio,
                inAppHost
            )
            this.webView = webView
            gestureListener.webView = webView
            webView.setOnTouchListener(this)
            webView.setOnLongClickListener(this)

            if (inAppNotification.isJsEnabled) {
                val instance = CleverTapAPI.instanceWithConfig(activity, config)
                webView.enableCTJavaScriptInterface(instance)
            }

            layout?.addView(webView)
            return inAppView
        } catch (t: Throwable) {
            config.getLogger().verbose(config.accountId, "Fragment view not created", t)
            return null
        }
    }

    private fun reDrawInApp() {
        val webView = this.webView ?: return
        webView.updateDimension()
        val html = inAppNotification.html ?: return
        webView.loadInAppHtml(html)
    }

    companion object {
        const val CTA_SWIPE_DISMISS = "swipe-dismiss"
        const val SWIPE_MIN_DISTANCE = 120
        const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}
