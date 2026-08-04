package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.clevertap.android.sdk.CTWebInterface
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.task.MainLooperHandler
import java.lang.ref.WeakReference

/**
 * Renders *custom-html* "header" and "footer" [CTInAppNotification]s in a [WindowManager] overlay,
 * used when the current Activity is not a [androidx.fragment.app.FragmentActivity] and the
 * fragment-based banner can therefore not be shown.
 *
 * **Why a WindowManager overlay and not the content view (as PIP does):** the hosts that need this
 * path are non-FragmentActivity surfaces such as game engines (Unreal/Unity), whose content view
 * is a `SurfaceView` driving a GL surface. A sibling view added to `android.R.id.content`
 * composites unreliably against a SurfaceView (z-ordering punches through), whereas a separate
 * overlay window sits cleanly above it.
 *
 * This class is UI-only. All analytics/action semantics are delegated to [host] (a
 * [CTHtmlBannerCallbacksBridge]), mirroring how the PIP renderer delegates to
 * [PIPInAppCallbacksBridge].
 */
internal class CTInAppHtmlBannerOverlay(
    private val notification: CTInAppNotification,
    private val config: CleverTapInstanceConfig,
    private val host: Callbacks,
    activity: Activity
) : View.OnTouchListener, View.OnLongClickListener {

    /**
     * The contract the overlay's host must satisfy. Extends [InAppWebInteraction] so the same
     * object drives the [CTInAppWebView]'s [InAppWebViewClient]/[CTWebInterface] and receives the
     * banner lifecycle callbacks below.
     */
    internal interface Callbacks : InAppWebInteraction {
        /** The overlay has been attached to the window. */
        fun onBannerShown()

        /** The user swiped the banner away; the host should run the close action. */
        fun onBannerSwipeDismissed()

        /** The overlay has been detached from the window; the host should report the dismiss. */
        fun onBannerRemoved()
    }

    companion object {
        fun canDisplay(type: CTInAppType?): Boolean {
            return type == CTInAppType.CTInAppTypeFooterHTML || type == CTInAppType.CTInAppTypeHeaderHTML
        }
    }

    private val activityWeakRef = WeakReference(activity)
    private val isJsEnabled = notification.isJsEnabled
    private val mainHandler = MainLooperHandler()
    private val gestureListener = PartialHtmlInAppGestureListener(
        scaledPixels = ::getScaledPixels,
        // Mark dismissing so the follow-up dismiss() from the close action removes immediately
        // instead of re-animating.
        onSwipeStart = { animatingDismiss = true },
        onSwipeDismiss = { host.onBannerSwipeDismissed() }
    )
    private val gd = GestureDetector(activity, gestureListener)

    private var wm: WindowManager? = null
    private var overlayRoot: View? = null
    private var webView: CTInAppWebView? = null
    private var animatingDismiss = false
    private var removed = false
    private var application: Application? = null

    /** The host activity, or null if collected. Used as the action activity-context. */
    val activity: Activity? get() = activityWeakRef.get()

    fun show() {
        mainHandler.post(this::build)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun build() {
        // This is partially based on CTInAppBasePartialHtmlFragment.displayHTMLView().
        val activity = activityWeakRef.get()
        val html = notification.html
        if (activity == null || html == null) {
            config.logger.debug(
                config.accountId,
                "CTInAppHtmlBannerOverlay: cannot show, activity or html is null"
            )
            // Unblock the controller's display queue.
            host.onBannerRemoved()
            return
        }

        try {
            val root = FrameLayout(activity)
            root.isClickable = true
            root.isFocusable = true
            overlayRoot = root

            val webView = CTInAppWebView(
                activity,
                notification.width,
                notification.height,
                notification.widthPercentage,
                notification.heightPercentage,
                notification.aspectRatio
            )
            this.webView = webView
            gestureListener.webView = webView
            webView.setWebViewClient(InAppWebViewClient(host))
            // Attach the swipe/pan gesture only when swipe-to-dismiss is enabled and there is no
            // close button, matching CTInAppBasePartialHtmlFragment.
            if (isSwipeToDismissEnabled()) {
                webView.setOnTouchListener(this)
            }
            webView.setOnLongClickListener(this)

            if (isJsEnabled) {
                val instance = CleverTapAPI.instanceWithConfig(activity, config)
                webView.setJavaScriptInterface(CTWebInterface(instance, host))
            }

            config.logger.verbose(
                config.accountId,
                "CTInAppHtmlBannerOverlay CTInAppNotification HTML:\n$html"
            )

            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                gravity()
            )
            root.addView(webView, lp)

            // A separate overlay window above the activity (see class docs for the rationale).
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

            // Announce the banner to accessibility services (parity with CTInAppBaseFragment).
            ViewCompat.setAccessibilityPaneTitle(
                root, activity.getString(R.string.ct_inapp_message_shown)
            )

            // Tear down the overlay if the host Activity is destroyed, so the window is not leaked
            // and the in-app display queue is not left wedged (a Fragment gets this for free).
            application = activity.application
            application?.registerActivityLifecycleCallbacks(lifecycleCallbacks)

            webView.updateDimension()
            webView.loadInAppHtml(html)
            host.onBannerShown()
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "CTInAppHtmlBannerOverlay: failed to show", t)
            // addView may have already attached the overlay before a later statement threw;
            // finishDismiss detaches it best-effort and reports the dismiss (guarded, so a later
            // teardown will not double-report).
            finishDismiss()
        }
    }

    override fun onLongClick(v: View): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gd.onTouchEvent(event) || (event.action == MotionEvent.ACTION_MOVE)
    }

    /**
     * Removes the overlay from the window. Safe to call from any thread (the JS bridge calls this
     * off the main thread). Reports [Callbacks.onBannerRemoved] once the view is detached.
     */
    fun dismiss() {
        mainHandler.post {
            val root = overlayRoot
            if (root == null || wm == null) {
                config.logger.debug(
                    config.accountId,
                    "CTInAppHtmlBannerOverlay.dismiss() - nothing to remove"
                )
                return@post
            }
            if (!animatingDismiss) {
                animatingDismiss = true
                root.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(this::finishDismiss)
                    .start()
            } else {
                finishDismiss()
            }
        }
    }

    private fun finishDismiss() {
        // One-shot: guards against a double removal (e.g. a re-entrant dismiss racing the fade's
        // withEndAction, a build() failure, or an activity-destroy teardown after a user dismiss)
        // reporting the dismiss twice.
        if (removed) {
            return
        }
        removed = true
        application?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        application = null
        try {
            wm?.removeViewImmediate(overlayRoot)
        } catch (e: Exception) {
            config.logger.debug(config.accountId, "CTInAppHtmlBannerOverlay: removing failed", e)
        } finally {
            overlayRoot = null
            cleanupWebView()
            host.onBannerRemoved()
        }
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityDestroyed(destroyed: Activity) {
            if (destroyed === activityWeakRef.get()) {
                // Host Activity is gone: remove the window (best-effort) and report the dismiss so
                // the controller clears currentlyDisplayingInApp and can show the next in-app.
                finishDismiss()
            }
        }

        override fun onActivityCreated(a: Activity, b: Bundle?) {}
        override fun onActivityStarted(a: Activity) {}
        override fun onActivityResumed(a: Activity) {}
        override fun onActivityPaused(a: Activity) {}
        override fun onActivityStopped(a: Activity) {}
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    }

    private fun cleanupWebView() {
        try {
            webView?.cleanup(isJsEnabled)
        } catch (e: Exception) {
            config.logger.debug(config.accountId, "CTInAppHtmlBannerOverlay: cleanup crash", e)
            // no-op; we are anyway destroying everything. This is just for safety.
        } finally {
            webView = null
        }
    }

    private fun isSwipeToDismissEnabled(): Boolean =
        !notification.isShowClose && notification.swipeToDismiss

    private fun getScaledPixels(raw: Int): Int {
        val activity = activityWeakRef.get() ?: return raw
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, raw.toFloat(), activity.resources.displayMetrics
        ).toInt()
    }

    private fun gravity(): Int {
        return if (notification.inAppType == CTInAppType.CTInAppTypeFooterHTML) {
            Gravity.BOTTOM
        } else {
            Gravity.TOP
        }
    }
}
