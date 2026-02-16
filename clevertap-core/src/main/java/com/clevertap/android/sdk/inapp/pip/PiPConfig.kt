package com.clevertap.android.sdk.inapp.pip

/**
 * Size presets for PiP window dimensions in dp.
 */
internal enum class PiPSizePreset(val widthDp: Int, val heightDp: Int) {
    SMALL(120, 68),
    MEDIUM(160, 90),
    LARGE(200, 112);

    companion object {
        fun fromString(value: String): PiPSizePreset = when (value.lowercase()) {
            "small", "s" -> SMALL
            "large", "l" -> LARGE
            else -> MEDIUM
        }
    }
}

/**
 * Corner positions for PiP initial placement and snap targets.
 */
internal enum class PiPCornerPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    companion object {
        fun fromString(value: String): PiPCornerPosition = when (value.lowercase()) {
            "top-left" -> TOP_LEFT
            "top-right" -> TOP_RIGHT
            "bottom-left" -> BOTTOM_LEFT
            else -> BOTTOM_RIGHT
        }
    }
}

/**
 * Animation types for PiP entry.
 */
internal enum class PiPEntryAnimation {
    INSTANT,
    DISSOLVE,
    MOVE_IN;

    companion object {
        fun fromString(value: String): PiPEntryAnimation = when (value.lowercase()) {
            "dissolve" -> DISSOLVE
            "move-in", "move_in" -> MOVE_IN
            else -> INSTANT
        }
    }
}
