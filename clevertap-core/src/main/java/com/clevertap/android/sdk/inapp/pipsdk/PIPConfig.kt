package com.clevertap.android.sdk.inapp.pipsdk

import android.graphics.Color
import com.clevertap.android.sdk.inapp.CTInAppAction

/**
 * Immutable configuration for a PIP session.
 *
 * Construct with named arguments:
 * ```
 * PIPConfig(mediaUrl = url, mediaType = PIPMediaType.IMAGE)
 * ```
 */
data class PIPConfig internal constructor(
    // Media
    val mediaUrl: String,
    val mediaType: PIPMediaType,
    val fallbackUrl: String? = null,            // static image shown if primary media fails
    val mediaContentDescription: String = "",  // from media.alt_text JSON field (accessibility)

    // Sizing
    val widthPercent: Int = 35,
    val aspectRatioNumerator: Int = 16,
    val aspectRatioDenominator: Int = 9,

    // Positioning
    val initialPosition: PIPPosition = PIPPosition.BOTTOM_RIGHT,
    val horizontalEdgeMarginDp: Int = 16,       // left/right margin from screen edges
    val verticalEdgeMarginDp: Int = 16,         // top/bottom margin from screen edges

    // Animation
    val animationConfig: PIPAnimationConfig = PIPAnimationConfig(),

    // Controls
    internal val action: CTInAppAction? = null, // null = action button hidden
    val showCloseButton: Boolean = true,        // false = close button hidden everywhere

    // Controls visibility (from server "controls" JSON object)
    val dragEnabled: Boolean = true,
    val showPlayPauseButton: Boolean = true,
    val showMuteButton: Boolean = true,
    val showExpandCollapseButton: Boolean = true,

    // Border & corner radius (image/GIF only; ignored for video)
    val cornerRadiusDp: Int = 0,
    val borderEnabled: Boolean = false,
    val borderColor: Int = Color.BLACK,
    val borderWidthDp: Int = 0,

    // Callbacks
    val callbacks: PIPCallbacks? = null,
) {
    init {
        require(mediaUrl.isNotBlank()) { "mediaUrl must not be blank" }
        require(widthPercent in 10..90) { "widthPercent must be 10–90, got $widthPercent" }
        require(aspectRatioNumerator > 0) { "aspectRatioNumerator must be > 0" }
        require(aspectRatioDenominator > 0) { "aspectRatioDenominator must be > 0" }
        require(horizontalEdgeMarginDp >= 0) { "horizontalEdgeMarginDp must be >= 0" }
        require(verticalEdgeMarginDp >= 0) { "verticalEdgeMarginDp must be >= 0" }
        require(cornerRadiusDp >= 0) { "cornerRadiusDp must be >= 0" }
        require(borderWidthDp >= 0) { "borderWidthDp must be >= 0" }
    }
}
