package com.clevertap.android.sdk.inapp

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import kotlin.math.abs

/**
 * Shared swipe-to-dismiss gesture for partial (header/footer) HTML in-apps, used by both the
 * fragment-hosted [com.clevertap.android.sdk.inapp.fragment.CTInAppBasePartialHtmlFragment] and the
 * non-fragment [CTInAppHtmlBannerOverlay].
 *
 * On a horizontal fling it slides + fades the current WebView out, then invokes [onSwipeDismiss].
 * [onSwipeStart] runs when the dismiss animation begins — the overlay uses it to coordinate its own
 * removal animation. [scaledPixels] converts dp to px in the host's context.
 *
 * The target is read through [webViewProvider] rather than held as state, so it always reflects the
 * host's live field (and becomes null once the host cleans up) — no stale reference to a destroyed
 * WebView.
 */
internal class PartialHtmlInAppGestureListener(
    private val webViewProvider: () -> CTInAppWebView?,
    private val scaledPixels: (Int) -> Int,
    private val onSwipeStart: () -> Unit = {},
    private val onSwipeDismiss: () -> Unit
) : SimpleOnGestureListener() {

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

    private fun remove(ltr: Boolean): Boolean {
        val webView = webViewProvider() ?: return false
        onSwipeStart()
        val animSet = AnimationSet(true)
        val anim = if (ltr) {
            TranslateAnimation(0f, scaledPixels(50).toFloat(), 0f, 0f)
        } else {
            TranslateAnimation(0f, -scaledPixels(50).toFloat(), 0f, 0f)
        }
        animSet.addAnimation(anim)
        animSet.addAnimation(AlphaAnimation(1f, 0f))
        animSet.duration = 300
        animSet.fillAfter = true
        animSet.isFillEnabled = true
        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                onSwipeDismiss()
            }

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationStart(animation: Animation?) {}
        })
        webView.startAnimation(animSet)
        return true
    }

    companion object {
        const val SWIPE_MIN_DISTANCE = 120
        const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}
