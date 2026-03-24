package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimationConfig

internal object PIPAnimator {

    private const val DURATION_SNAP_MS = 250L
    private const val DURATION_EXPAND_MS = 250L
    private const val DURATION_COLLAPSE_MS = 200L
    private const val EXPAND_INITIAL_SCALE = 0.85f
    private const val SNAP_OVERSHOOT_TENSION = 1.2f
    /** Fraction of container used to determine which edge to slide from/to in MOVE_IN animation. */
    private const val EDGE_ZONE_FRACTION = 0.33f

    fun animateIn(
        view: View,
        anchor: PointF,
        animationConfig: PIPAnimationConfig,
        containerWidth: Int,
        containerHeight: Int,
        onComplete: () -> Unit = {},
    ) {
        val duration = animationConfig.durationMs
        val interpolator = animationConfig.interpolator

        when (animationConfig.type) {
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
                view.animate().alpha(1f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction(onComplete).start()
            }
            PIPAnimation.MOVE_IN -> {
                val startOffset = moveInStartOffset(
                    anchor, view.width, view.height, containerWidth, containerHeight,
                    animationConfig.moveInDirection,
                )
                view.x = anchor.x + startOffset.x
                view.y = anchor.y + startOffset.y
                view.alpha = 1f
                view.visibility = View.VISIBLE
                view.animate()
                    .x(anchor.x).y(anchor.y)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction(onComplete).start()
            }
        }
    }

    fun animateOut(view: View, animationConfig: PIPAnimationConfig, onComplete: () -> Unit) {
        val duration = animationConfig.durationMs
        val interpolator = animationConfig.interpolator

        when (animationConfig.type) {
            PIPAnimation.INSTANT -> {
                view.visibility = View.GONE
                onComplete()
            }
            PIPAnimation.DISSOLVE -> {
                view.animate().alpha(0f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction { view.visibility = View.GONE; onComplete() }.start()
            }
            PIPAnimation.MOVE_IN -> {
                // Exit direction is always auto-derived from current position (nearest edge)
                val parent = view.parent as? ViewGroup
                val delta = slideOutDelta(view, parent?.width ?: 0, parent?.height ?: 0)
                view.animate()
                    .xBy(delta.x).yBy(delta.y)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .withEndAction { view.visibility = View.GONE; onComplete() }.start()
            }
        }
    }

    fun animateSnap(view: View, targetX: Float, targetY: Float, onComplete: () -> Unit) {
        view.animate()
            .x(targetX).y(targetY)
            .setDuration(DURATION_SNAP_MS)
            .setInterpolator(OvershootInterpolator(SNAP_OVERSHOOT_TENSION))
            .withEndAction(onComplete)
            .start()
    }

    fun animateExpand(overlay: View, mediaContainer: View, onComplete: () -> Unit) {
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        mediaContainer.scaleX = EXPAND_INITIAL_SCALE
        mediaContainer.scaleY = EXPAND_INITIAL_SCALE
        overlay.animate().alpha(1f).setDuration(DURATION_EXPAND_MS).start()
        mediaContainer.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(DURATION_EXPAND_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onComplete).start()
    }

    fun animateCollapse(overlay: View, onComplete: () -> Unit) {
        overlay.animate().alpha(0f).setDuration(DURATION_COLLAPSE_MS)
            .withEndAction { overlay.visibility = View.GONE; onComplete() }.start()
    }

    /**
     * Calculates the off-screen start offset for MOVE_IN entry animation.
     *
     * If [direction] is set (from server config), it takes precedence.
     * Otherwise, direction is auto-derived from the anchor position relative to the container.
     */
    private fun moveInStartOffset(
        anchor: PointF, pipW: Int, pipH: Int, containerW: Int, containerH: Int,
        direction: PIPAnimationConfig.MoveInDirection?,
    ): PointF {
        if (direction != null) {
            return when (direction) {
                PIPAnimationConfig.MoveInDirection.LEFT -> PointF(-(anchor.x + pipW), 0f)
                PIPAnimationConfig.MoveInDirection.RIGHT -> PointF(containerW - anchor.x, 0f)
                PIPAnimationConfig.MoveInDirection.TOP -> PointF(0f, -(anchor.y + pipH))
                PIPAnimationConfig.MoveInDirection.BOTTOM -> PointF(0f, containerH - anchor.y)
            }
        }

        // Auto-derive from anchor position
        val cx = anchor.x + pipW / 2f
        val cy = anchor.y + pipH / 2f
        return when {
            cy < containerH * EDGE_ZONE_FRACTION -> PointF(0f, -(anchor.y + pipH))
            cy > containerH * (1f - EDGE_ZONE_FRACTION) -> PointF(0f, containerH - anchor.y)
            cx < containerW * EDGE_ZONE_FRACTION -> PointF(-(anchor.x + pipW), 0f)
            cx > containerW * (1f - EDGE_ZONE_FRACTION) -> PointF(containerW - anchor.x, 0f)
            else -> PointF(0f, 0f)
        }
    }

    private fun slideOutDelta(view: View, containerW: Int, containerH: Int): PointF {
        val cx = view.x + view.width / 2f
        val cy = view.y + view.height / 2f
        return when {
            cy < containerH * EDGE_ZONE_FRACTION -> PointF(0f, -(view.y + view.height))
            cy > containerH * (1f - EDGE_ZONE_FRACTION) -> PointF(0f, (containerH - view.y))
            cx < containerW * EDGE_ZONE_FRACTION -> PointF(-(view.x + view.width), 0f)
            else -> PointF((containerW - view.x), 0f)
        }
    }
}
