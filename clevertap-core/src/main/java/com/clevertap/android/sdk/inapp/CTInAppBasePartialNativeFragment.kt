package com.clevertap.android.sdk.inapp

import android.content.Context
import android.graphics.Color
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
import android.widget.Button
import android.widget.LinearLayout
import kotlin.math.abs
import androidx.core.graphics.toColorInt

internal abstract class CTInAppBasePartialNativeFragment : CTInAppBasePartialFragment(),
    OnTouchListener, OnLongClickListener {

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (e1 == null) {
                return false
            }
            if (e1.x - e2.x > SWIPE_MIN_DISTANCE && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                // Right to left
                return remove(ltr = false)
            } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                // Left to right
                return remove(ltr = true)
            }
            return false
        }

        fun remove(ltr: Boolean): Boolean {
            val animSet = AnimationSet(true)
            val anim: TranslateAnimation?
            if (ltr) {
                anim = TranslateAnimation(0f, getScaledPixels(50).toFloat(), 0f, 0f)
            } else {
                anim = TranslateAnimation(0f, -getScaledPixels(50).toFloat(), 0f, 0f)
            }
            animSet.addAnimation(anim)
            animSet.addAnimation(AlphaAnimation(1f, 0f))
            animSet.setDuration(300)
            animSet.setFillAfter(true)
            animSet.isFillEnabled = true
            animSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    didDismiss(null)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationStart(animation: Animation?) {
                }
            })
            inAppView?.startAnimation(animSet)
            return true
        }
    }

    protected lateinit var gd: GestureDetector
    protected var inAppView: View? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gd = GestureDetector(context, GestureListener())
    }

    override fun onLongClick(v: View?): Boolean {
        return true
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return gd.onTouchEvent(event) || (event.action == MotionEvent.ACTION_MOVE)
    }

    fun hideSecondaryButton(mainButton: Button, secondaryButton: Button) {
        secondaryButton.visibility = View.GONE
        val mainLayoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 2f
        )
        mainButton.setLayoutParams(mainLayoutParams)
        val secondaryLayoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 0f
        )
        secondaryButton.setLayoutParams(secondaryLayoutParams)
    }

    fun setupInAppButton(
        inAppButton: Button, inAppNotificationButton: CTInAppNotificationButton?, buttonIndex: Int
    ) {
        if (inAppNotificationButton != null) {
            inAppButton.tag = buttonIndex
            inAppButton.visibility = View.VISIBLE
            inAppButton.text = inAppNotificationButton.text
            inAppButton.setTextColor(inAppNotificationButton.textColor.toColorInt())
            inAppButton.setBackgroundColor(inAppNotificationButton.backgroundColor.toColorInt())
            inAppButton.setOnClickListener(CTInAppNativeButtonClickListener())
        } else {
            inAppButton.visibility = View.GONE
        }
    }
}
