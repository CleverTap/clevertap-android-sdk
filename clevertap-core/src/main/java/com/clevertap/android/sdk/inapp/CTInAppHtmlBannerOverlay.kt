package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.task.MainLooperHandler

import java.lang.ref.WeakReference

/**
 * Class that renders *custom-html* "header" and "footer" CTInAppNotifications
 * using an overlay window instead of the normal Fragment-based banner.
 * <br>
 * This allows this type of notification to be used in any Activity,
 * whereas the normal CTInAppHtmlHeaderFragment and CTInAppHtmlFooterFragment
 * require a FragmentActivity.
 **/
internal class CTInAppHtmlBannerOverlay(
    private val notification: CTInAppNotification,
    private val config: CleverTapInstanceConfig,
    inAppListener: InAppListener,
    activity: Activity
) : View.OnTouchListener, View.OnLongClickListener,
    CTInAppHost.Callbacks {

    companion object {
        fun canDisplay(type: CTInAppType): Boolean {
            return type == CTInAppType.CTInAppTypeFooterHTML || type == CTInAppType.CTInAppTypeHeaderHTML
        }

        fun show(
            notification: CTInAppNotification,
            config: CleverTapInstanceConfig,
            inAppListener: InAppListener,
            activity: Activity
        ) {
            CTInAppHtmlBannerOverlay(notification, config, inAppListener, activity).show()
        }
    }

    private val activityWeakRef = WeakReference(activity)
    private val isJsEnabled = notification.isJsEnabled
    private val mainHandler = MainLooperHandler()
    private val inAppHost = CTInAppHost(inAppListener, config, notification, this, activity)
    private val gestureListener = PartialHtmlInAppGestureListener(inAppHost)
    private val gd = GestureDetector(gestureListener)
    private var wm: WindowManager? = null
    private var overlayRoot: View? = null
    private var webView: CTInAppWebView? = null
    private var animatingDismiss = false

    fun show() {
        mainHandler.post(this::build)
    }

    private fun build() {
        // this code partially based on CTInAppBasePartialHtmlFragment.displayHTMLView()
        val activity = activityWeakRef.get() ?: return
        val root = FrameLayout(activity)
        root.isClickable = true
        root.setFocusable(true)
        overlayRoot = root

        // ---------- WebView ----------
        val webView = CTInAppWebView(
            activity,
            notification.width,
            notification.height,
            notification.widthPercentage,
            notification.heightPercentage,
            notification.aspectRatio,
            inAppHost
        )
        this.webView = webView
        gestureListener.webView = webView
        webView.setOnTouchListener(this)
        webView.setOnLongClickListener(this)

        // Install our custom JavaScript interface
        if (isJsEnabled) {
            val instance = CleverTapAPI.instanceWithConfig(activity, config)
            webView.enableCTJavaScriptInterface(instance)
        }

        // load the HTML
        val html = notification.html ?: return
        config.getLogger().verbose(
            config.accountId,
            "CTInAppHtmlBannerOverlay CTInAppNotification HTML:\n$html"
        )

        // add to the layout
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            gravity()
        )
        root.addView(webView, lp)

        // add the overlay to the activity's window manager
        val wmlp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, // fit the webview
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, // sit above with own input stream
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        wmlp.gravity = gravity() // TOP or BOTTOM
        wmlp.token = activity.window.decorView.windowToken // tie to this activity
        wm = activity.windowManager
        wm?.addView(root, wmlp)

        webView.updateDimension()
        webView.loadInAppHtml(html)
        inAppHost.didShowInApp(null)
    }

    override fun onLongClick(v: View): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gd.onTouchEvent(event) || (event.action == MotionEvent.ACTION_MOVE)
    }

    override fun onDismissInApp() {
        dismiss()
    }

    private fun dismiss() {
        val overlayRoot = this.overlayRoot
        if (overlayRoot == null || wm == null) {
            config.getLogger().debug(
                config.accountId,
                "CTInAppHtmlBannerOverlay.dismiss() - Missing overlay or window manager"
            )
            return
        }
        if (!animatingDismiss) {
            animatingDismiss = true
            overlayRoot.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(this::finishDismiss)
                .start()
        } else {
            finishDismiss()
        }
    }

    private fun finishDismiss() {
        mainHandler.post {
            try {
                wm?.removeViewImmediate(overlayRoot)
                overlayRoot = null
                cleanupWebView()
            } catch (exception: Exception) {
                config.getLogger().debug(
                    config.accountId,
                    "CTInAppHtmlBannerOverlay: Removing failed!",
                    exception
                )
            }
        }
    }

    private fun cleanupWebView() {
        try {
            webView?.cleanup(isJsEnabled)
            webView = null
        } catch (e: Exception) {
            config.getLogger().debug("cleanupWebView -> there was some crash in cleanup", e)
            // no-op; we are anyway destroying everything. This is just for safety.
        }
    }

    private fun gravity(): Int {
        return if (CTInAppType.CTInAppTypeFooterHTML == notification.inAppType) {
            Gravity.BOTTOM
        } else {
            Gravity.TOP
        }
    }
}
