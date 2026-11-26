package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
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
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton
import androidx.annotation.RestrictTo

@Suppress("UnnecessaryAbstractClass") // shared base of the native header/footer fragments
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class CTInAppBasePartialNativeFragment : CTInAppBasePartialFragment(),
    OnTouchListener, OnLongClickListener {

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_THRESHOLD_VELOCITY = 200
        private const val SWIPE_DISMISS_DISTANCE = 50
        private const val SWIPE_DISMISS_DURATION_MS = 300L
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
                anim = TranslateAnimation(0f, getScaledPixels(SWIPE_DISMISS_DISTANCE).toFloat(), 0f, 0f)
            } else {
                anim = TranslateAnimation(0f, -getScaledPixels(SWIPE_DISMISS_DISTANCE).toFloat(), 0f, 0f)
            }
            animSet.addAnimation(anim)
            animSet.addAnimation(AlphaAnimation(1f, 0f))
            animSet.setDuration(SWIPE_DISMISS_DURATION_MS)
            animSet.setFillAfter(true)
            animSet.isFillEnabled = true
            animSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    triggerSwipeDismissAction()
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // No-op: only the end of the dismiss animation matters.
                }

                override fun onAnimationStart(animation: Animation?) {
                    // No-op: only the end of the dismiss animation matters.
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
