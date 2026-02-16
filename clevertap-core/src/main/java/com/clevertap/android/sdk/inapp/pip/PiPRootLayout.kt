package com.clevertap.android.sdk.inapp.pip

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * A transparent root layout that does not intercept touch events,
 * allowing the underlying activity to receive them. Only child views
 * (the PiP container, expanded container) will handle their own touches.
 */
internal class PiPRootLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false

    override fun onTouchEvent(event: MotionEvent): Boolean = false
}
