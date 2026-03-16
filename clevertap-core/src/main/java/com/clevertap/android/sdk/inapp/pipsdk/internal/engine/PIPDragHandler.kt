package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import kotlin.math.abs

/**
 * Handles drag-to-reposition and tap detection for [com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPCompactView].
 *
 * Usage: call [onInterceptDown] from [View.onInterceptTouchEvent] ACTION_DOWN,
 * [shouldIntercept] for ACTION_MOVE, and [onTouchEvent] from [View.onTouchEvent].
 */
internal class PIPDragHandler(
    private val view: View,
    private val getHorizontalEdgeMarginDp: () -> Int,
    private val getVerticalEdgeMarginDp: () -> Int,
    private val onSnapComplete: (PIPPosition) -> Unit,
    private val onTap: () -> Unit,
) {
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var viewStartX = 0f
    private var viewStartY = 0f
    private var isDragging = false

    /** Call from onInterceptTouchEvent ACTION_DOWN to record starting coords. */
    fun onInterceptDown(event: MotionEvent) {
        touchStartX = event.rawX
        touchStartY = event.rawY
        viewStartX = view.x
        viewStartY = view.y
        isDragging = false
    }

    /** Returns true once drag threshold is exceeded — triggers intercept. */
    fun shouldIntercept(event: MotionEvent): Boolean {
        val dx = abs(event.rawX - touchStartX)
        val dy = abs(event.rawY - touchStartY)
        if (!isDragging && (dx > DRAG_THRESHOLD_PX || dy > DRAG_THRESHOLD_PX)) {
            isDragging = true
        }
        return isDragging
    }

    /** Call from onTouchEvent to handle move/up after interception. */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    val dx = abs(event.rawX - touchStartX)
                    val dy = abs(event.rawY - touchStartY)
                    isDragging = dx > DRAG_THRESHOLD_PX || dy > DRAG_THRESHOLD_PX
                }
                if (isDragging) {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    view.x = viewStartX + dx
                    view.y = viewStartY + dy
                }
                isDragging
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    snapToNearest()
                } else {
                    onTap()
                }
                isDragging = false
                true
            }
            else -> false
        }
    }

    private fun snapToNearest() {
        val parent = view.parent as? ViewGroup ?: return
        val hMarginPx = getHorizontalEdgeMarginDp().dpToPx(view.context)
        val vMarginPx = getVerticalEdgeMarginDp().dpToPx(view.context)
        val anchors = PIPPositionResolver.resolveAnchors(
            parent.width, parent.height,
            view.width, view.height,
            hMarginPx, vMarginPx,
        )
        val centerX = view.x + view.width / 2f
        val centerY = view.y + view.height / 2f
        val target = PIPPositionResolver.nearestPosition(centerX, centerY, anchors, view.width, view.height)
        val anchor = anchors[target] ?: return
        PIPAnimator.animateSnap(view, anchor.x, anchor.y) {
            onSnapComplete(target)
        }
    }

    companion object {
        private const val DRAG_THRESHOLD_PX = 12f
    }
}
