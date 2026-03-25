package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Transparent overlay that hosts control buttons and handles auto-hide after [AUTO_HIDE_DELAY_MS].
 *
 * Parent views add buttons directly to this container. Call [showControls] to reveal
 * (with optional auto-hide), [hideControls] to immediately hide, and [detach] on cleanup
 * to cancel pending callbacks.
 */
internal class PIPControlsOverlay(context: Context) : FrameLayout(context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }
    private var controlsVisible = false

    /** Block all touch events to children when controls are hidden (alpha=0 doesn't disable clicks). */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return !controlsVisible
    }

    fun showControls(autoHide: Boolean = true) {
        mainHandler.removeCallbacks(hideRunnable)
        animate().cancel()
        controlsVisible = true
        animate().alpha(1f).setDuration(CONTROLS_FADE_DURATION_MS).start()
        if (autoHide) mainHandler.postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun hideControls() {
        mainHandler.removeCallbacks(hideRunnable)
        animate().cancel()
        animate().alpha(0f).setDuration(CONTROLS_FADE_DURATION_MS)
            .withEndAction { controlsVisible = false }
            .start()
    }

    fun resetAutoHideTimer() {
        mainHandler.removeCallbacks(hideRunnable)
        mainHandler.postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun detach() = mainHandler.removeCallbacks(hideRunnable)

    companion object {
        private const val AUTO_HIDE_DELAY_MS = 3_000L
        private const val CONTROLS_FADE_DURATION_MS = 200L
    }
}
