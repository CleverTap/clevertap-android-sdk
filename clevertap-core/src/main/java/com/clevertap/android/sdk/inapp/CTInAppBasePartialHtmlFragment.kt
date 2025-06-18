package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import com.clevertap.android.sdk.CTWebInterface
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppAction.CREATOR.createCloseAction
import kotlin.math.abs

internal abstract class CTInAppBasePartialHtmlFragment : CTInAppBasePartialFragment(),
    OnTouchListener,
    OnLongClickListener {

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 != null) {
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                    // Right to left
                    return remove(false)
                } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                    // Left to right
                    return remove(true)
                }
            }
            return false
        }

        fun remove(ltr: Boolean): Boolean {
            val animSet = AnimationSet(true)
            val anim = if (ltr) {
                TranslateAnimation(0f, getScaledPixels(50).toFloat(), 0f, 0f)
            } else {
                TranslateAnimation(0f, -getScaledPixels(50).toFloat(), 0f, 0f)
            }
            animSet.addAnimation(anim)
            animSet.addAnimation(AlphaAnimation(1f, 0f))
            animSet.setDuration(300)
            animSet.setFillAfter(true)
            animSet.isFillEnabled = true
            animSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    triggerAction(createCloseAction(), CTA_SWIPE_DISMISS, null)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationStart(animation: Animation?) {
                }
            })
            webView?.startAnimation(animSet)
            return true
        }
    }

    private lateinit var gd: GestureDetector

    private var webView: CTInAppWebView? = null


    abstract fun getLayout(view: View?): ViewGroup?

    abstract fun getView(inflater: LayoutInflater, container: ViewGroup?): View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gd = GestureDetector(context, GestureListener())
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
                inAppNotification.aspectRatio
            )
            this.webView = webView
            val webViewClient = InAppWebViewClient(this)
            webView.setWebViewClient(webViewClient)
            webView.setOnTouchListener(this)
            webView.setOnLongClickListener(this)

            if (inAppNotification.isJsEnabled) {
                val instance = CleverTapAPI.instanceWithConfig(activity, config)
                val ctWebInterface = CTWebInterface(instance, this)
                webView.setJavaScriptInterface(ctWebInterface)
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

        var mHeight = webView.dim.y
        var mWidth = webView.dim.x

        val d = resources.displayMetrics.density
        mHeight = (mHeight / d).toInt()
        mWidth = (mWidth / d).toInt()

        var html = inAppNotification.html ?: return

        val style =
            "<style>body{width: ${mWidth}px; height: ${mHeight}px; margin: 0; padding:0;}</style>"
        html = html.replaceFirst("<head>".toRegex(), "<head>$style")
        Logger.v("Density appears to be $d")

        webView.setInitialScale((d * 100).toInt())
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    companion object {
        private const val CTA_SWIPE_DISMISS = "swipe-dismiss"
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}
