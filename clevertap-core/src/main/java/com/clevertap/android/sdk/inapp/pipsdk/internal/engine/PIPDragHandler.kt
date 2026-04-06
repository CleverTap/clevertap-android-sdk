package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.graphics.Insets
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
    private val dragEnabled: Boolean = true,
    private val getHorizontalEdgeMarginPercent: () -> Int,
    private val getVerticalEdgeMarginPercent: () -> Int,
    private val getSafeInsets: () -> Insets = { Insets.NONE },
    private val getBottomOffsetPx: () -> Int = { 0 },
    private val onSnapComplete: (PIPPosition) -> Unit,
    private val onTap: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
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
        if (!dragEnabled) return false
        val dx = abs(event.rawX - touchStartX)
        val dy = abs(event.rawY - touchStartY)
        if (!isDragging && (dx > touchSlop || dy > touchSlop)) {
            isDragging = true
        }
        return isDragging
    }

    /** Call from onTouchEvent to handle move/up after interception. */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!dragEnabled) return false
                if (!isDragging) {
                    val dx = abs(event.rawX - touchStartX)
                    val dy = abs(event.rawY - touchStartY)
                    isDragging = dx > touchSlop || dy > touchSlop
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
        val hMarginPx = getHorizontalEdgeMarginPercent().percentOf(parent.width)
        val vMarginPx = getVerticalEdgeMarginPercent().percentOf(parent.height)
        val anchors = PIPPositionResolver.resolveAnchors(
            parent.width, parent.height,
            view.width, view.height,
            hMarginPx, vMarginPx,
            getSafeInsets(),
            getBottomOffsetPx(),
        )
        val centerX = view.x + view.width / 2f
        val centerY = view.y + view.height / 2f
        val target = PIPPositionResolver.nearestPosition(centerX, centerY, anchors, view.width, view.height)
        val anchor = anchors[target] ?: return
        PIPAnimator.animateSnap(view, anchor.x, anchor.y) {
            onSnapComplete(target)
        }
    }
}
