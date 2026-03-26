package com.clevertap.android.sdk.inapp.pipsdk

import android.animation.TimeInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Immutable animation configuration for a PIP session.
 *
 * Holds the animation type, duration, easing interpolator, and optional move-in direction.
 * Parsed from the server's `animation` JSON object by `PIPConfigFactory`.
 */
data class PIPAnimationConfig(
    val type: PIPAnimation = PIPAnimation.DISSOLVE,
    val durationMs: Long = DEFAULT_DURATION_MS,
    val interpolator: TimeInterpolator = DEFAULT_INTERPOLATOR,
    val moveInDirection: MoveInDirection? = null,
) {
    init {
        require(durationMs >= 0) { "durationMs must be non-negative, was $durationMs" }
    }

    /** Explicit direction for MOVE_IN entry animation. */
    enum class MoveInDirection { LEFT, RIGHT, TOP, BOTTOM }

    companion object {
        const val DEFAULT_DURATION_MS = 300L
        val DEFAULT_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()
    }
}
