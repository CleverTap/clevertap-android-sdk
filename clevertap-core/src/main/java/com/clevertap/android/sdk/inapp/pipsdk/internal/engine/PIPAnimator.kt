package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation

internal object PIPAnimator {

    fun animateIn(
        view: View,
        anchor: PointF,
        animation: PIPAnimation,
        containerWidth: Int,
        containerHeight: Int,
        onComplete: () -> Unit = {},
    ) {
        when (animation) {
            PIPAnimation.INSTANT -> {
                view.x = anchor.x
                view.y = anchor.y
                view.alpha = 1f
                view.visibility = View.VISIBLE
                onComplete()
            }
            PIPAnimation.DISSOLVE -> {
                view.x = anchor.x
                view.y = anchor.y
                view.alpha = 0f
                view.visibility = View.VISIBLE
                view.animate().alpha(1f).setDuration(300)
                    .withEndAction(onComplete).start()
            }
            PIPAnimation.MOVE_IN -> {
                val startOffset = moveInStartOffset(
                    anchor, view.width, view.height, containerWidth, containerHeight,
                )
                view.x = anchor.x + startOffset.x
                view.y = anchor.y + startOffset.y
                view.alpha = 1f
                view.visibility = View.VISIBLE
                view.animate()
                    .x(anchor.x).y(anchor.y)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction(onComplete).start()
            }
        }
    }

    fun animateOut(view: View, animation: PIPAnimation, onComplete: () -> Unit) {
        when (animation) {
            PIPAnimation.INSTANT -> {
                view.visibility = View.GONE
                onComplete()
            }
            PIPAnimation.DISSOLVE -> {
                view.animate().alpha(0f).setDuration(250)
                    .withEndAction { view.visibility = View.GONE; onComplete() }.start()
            }
            PIPAnimation.MOVE_IN -> {
                val parent = view.parent as? ViewGroup
                val delta = slideOutDelta(view, parent?.width ?: 0, parent?.height ?: 0)
                view.animate()
                    .xBy(delta.x).yBy(delta.y)
                    .setDuration(300)
                    .withEndAction { view.visibility = View.GONE; onComplete() }.start()
            }
        }
    }

    fun animateSnap(view: View, targetX: Float, targetY: Float, onComplete: () -> Unit) {
        view.animate()
            .x(targetX).y(targetY)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction(onComplete)
            .start()
    }

    fun animateExpand(overlay: View, mediaContainer: View, onComplete: () -> Unit) {
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        mediaContainer.scaleX = 0.85f
        mediaContainer.scaleY = 0.85f
        overlay.animate().alpha(1f).setDuration(250).start()
        mediaContainer.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onComplete).start()
    }

    fun animateCollapse(overlay: View, onComplete: () -> Unit) {
        overlay.animate().alpha(0f).setDuration(200)
            .withEndAction { overlay.visibility = View.GONE; onComplete() }.start()
    }

    private fun moveInStartOffset(
        anchor: PointF, pipW: Int, pipH: Int, containerW: Int, containerH: Int,
    ): PointF {
        val cx = anchor.x + pipW / 2f
        val cy = anchor.y + pipH / 2f
        return when {
            cy < containerH * 0.33f -> PointF(0f, -(anchor.y + pipH))
            cy > containerH * 0.66f -> PointF(0f, containerH - anchor.y)
            cx < containerW * 0.33f -> PointF(-(anchor.x + pipW), 0f)
            cx > containerW * 0.66f -> PointF(containerW - anchor.x, 0f)
            else -> PointF(0f, 0f)
        }
    }

    private fun slideOutDelta(view: View, containerW: Int, containerH: Int): PointF {
        val cx = view.x + view.width / 2f
        val cy = view.y + view.height / 2f
        return when {
            cy < containerH * 0.33f -> PointF(0f, -(view.y + view.height))
            cy > containerH * 0.66f -> PointF(0f, (containerH - view.y))
            cx < containerW * 0.33f -> PointF(-(view.x + view.width), 0f)
            else -> PointF((containerW - view.x), 0f)
        }
    }
}
