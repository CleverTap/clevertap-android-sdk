package com.clevertap.android.sdk.inapp.pipsdk

/**
 * Border configuration for the compact PIP view.
 *
 * @param enabled    Whether to draw a border around the compact view. Default: false.
 * @param color      Hex color string for the border (e.g. "#FFFFFF"). Default: white.
 * @param widthDp    Border stroke width in dp. Default: 1.
 */
data class PIPBorderConfig(
    val enabled: Boolean = false,
    val color: String = "#FFFFFF",
    val widthDp: Int = 1,
)
