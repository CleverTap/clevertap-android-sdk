package com.clevertap.android.sdk.inapp

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import com.clevertap.android.sdk.inapp.CTInAppAction.CREATOR.createCloseAction
import com.clevertap.android.sdk.inapp.fragment.CTInAppBasePartialHtmlFragment.Companion.CTA_SWIPE_DISMISS
import com.clevertap.android.sdk.inapp.fragment.CTInAppBasePartialHtmlFragment.Companion.SWIPE_MIN_DISTANCE
import com.clevertap.android.sdk.inapp.fragment.CTInAppBasePartialHtmlFragment.Companion.SWIPE_THRESHOLD_VELOCITY
import kotlin.math.abs

internal class PartialHtmlInAppGestureListener(private val inAppHost: CTInAppHost) :
    SimpleOnGestureListener() {

    var webView: CTInAppWebView? = null

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
        val animSet = AnimationSet(true)
        val anim = if (ltr) {
            TranslateAnimation(0f, inAppHost.getScaledPixels(50).toFloat(), 0f, 0f)
        } else {
            TranslateAnimation(0f, -inAppHost.getScaledPixels(50).toFloat(), 0f, 0f)
        }
        animSet.addAnimation(anim)
        animSet.addAnimation(AlphaAnimation(1f, 0f))
        animSet.setDuration(300)
        animSet.setFillAfter(true)
        animSet.isFillEnabled = true
        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                inAppHost.triggerAction(createCloseAction(), CTA_SWIPE_DISMISS, null)
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
