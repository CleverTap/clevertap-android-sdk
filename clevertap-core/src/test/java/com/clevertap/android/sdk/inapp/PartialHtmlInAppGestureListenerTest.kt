package com.clevertap.android.sdk.inapp

import android.view.MotionEvent
import android.view.animation.Animation
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PartialHtmlInAppGestureListenerTest {

    private val webView = mockk<CTInAppWebView>(relaxed = true)
    private var swipeStartCount = 0
    private var swipeDismissCount = 0

    private fun listener(
        webViewProvider: () -> CTInAppWebView? = { webView }
    ) = PartialHtmlInAppGestureListener(
        webViewProvider = webViewProvider,
        scaledPixels = { it },
        onSwipeStart = { swipeStartCount++ },
        onSwipeDismiss = { swipeDismissCount++ }
    )

    private fun event(x: Float) = mockk<MotionEvent>(relaxed = true).also { every { it.x } returns x }

    @Test
    fun `horizontal fling beyond both thresholds starts the swipe dismiss`() {
        // dx = 200 (> 120), |velocity| = 201 (> 200)
        val consumed = listener().onFling(event(200f), event(0f), -201f, 0f)

        assertTrue(consumed)
        assertEquals(1, swipeStartCount)
        verify(exactly = 1) { webView.startAnimation(any()) }
        // onSwipeDismiss fires only at animation end, not synchronously.
        assertEquals(0, swipeDismissCount)
    }

    @Test
    fun `fling below the distance threshold is ignored`() {
        // dx = 119 (< 120), velocity well past its threshold
        val consumed = listener().onFling(event(119f), event(0f), -400f, 0f)

        assertFalse(consumed)
        assertEquals(0, swipeStartCount)
        verify(exactly = 0) { webView.startAnimation(any()) }
    }

    @Test
    fun `fling below the velocity threshold is ignored`() {
        // dx well past its threshold, |velocity| = 199 (< 200)
        val consumed = listener().onFling(event(200f), event(0f), -199f, 0f)

        assertFalse(consumed)
        assertEquals(0, swipeStartCount)
    }

    @Test
    fun `null first event is ignored`() {
        val consumed = listener().onFling(null, event(0f), -400f, 0f)

        assertFalse(consumed)
        assertEquals(0, swipeStartCount)
    }

    @Test
    fun `a null webView aborts before the swipe starts`() {
        val consumed = listener(webViewProvider = { null }).onFling(event(200f), event(0f), -400f, 0f)

        assertFalse(consumed)
        assertEquals(0, swipeStartCount)
    }

    @Test
    fun `swipe dismiss fires at animation end, strictly after swipe start`() {
        val order = mutableListOf<String>()
        val gestureListener = PartialHtmlInAppGestureListener(
            webViewProvider = { webView },
            scaledPixels = { it },
            onSwipeStart = { order += "start" },
            onSwipeDismiss = { order += "dismiss" }
        )
        val animSlot = slot<Animation>()
        every { webView.startAnimation(capture(animSlot)) } just runs

        val consumed = gestureListener.onFling(event(200f), event(0f), -201f, 0f)

        // onSwipeStart ran synchronously; onSwipeDismiss has NOT fired yet.
        assertTrue(consumed)
        assertEquals(listOf("start"), order)

        // Drive the captured animation to completion -> onSwipeDismiss fires, strictly after start.
        animationListenerOf(animSlot.captured).onAnimationEnd(animSlot.captured)
        assertEquals(listOf("start", "dismiss"), order)
    }

    /** Reads the [Animation.AnimationListener] the gesture attached to its [AnimationSet]. */
    private fun animationListenerOf(animation: Animation): Animation.AnimationListener {
        val field = Animation::class.java.getDeclaredField("mListener")
        field.isAccessible = true
        return field.get(animation) as Animation.AnimationListener
    }
}
