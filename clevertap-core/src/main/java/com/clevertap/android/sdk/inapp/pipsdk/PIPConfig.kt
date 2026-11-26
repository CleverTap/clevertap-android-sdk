package com.clevertap.android.sdk.inapp.pipsdk

import android.graphics.Color
import com.clevertap.android.sdk.inapp.CTInAppAction
import androidx.annotation.RestrictTo

/**
 * Immutable configuration for a PIP session.
 *
 * Construct with named arguments:
 * ```
 * PIPConfig(mediaUrl = url, mediaType = PIPMediaType.IMAGE)
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class PIPConfig internal constructor(
    // Media
    val mediaUrl: String,
    val mediaType: PIPMediaType,
    val fallbackUrl: String? = null,            // static image shown if primary media fails
    val mediaContentDescription: String = "",  // from media.alt_text JSON field (accessibility)

    // Sizing
    val widthPercent: Int = DEFAULT_WIDTH_PERCENT,
    val aspectRatioNumerator: Double = DEFAULT_ASPECT_RATIO_NUMERATOR,
    val aspectRatioDenominator: Double = DEFAULT_ASPECT_RATIO_DENOMINATOR,

    // Positioning
    val initialPosition: PIPPosition = PIPPosition.BOTTOM_RIGHT,
    val horizontalEdgeMarginPercent: Int = DEFAULT_EDGE_MARGIN_PERCENT, // left/right margin as % of screen width
    val verticalEdgeMarginPercent: Int = DEFAULT_EDGE_MARGIN_PERCENT, // top/bottom margin as % of screen height

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
        require(widthPercent in MIN_WIDTH_PERCENT..MAX_WIDTH_PERCENT) {
            "widthPercent must be 10–90, got $widthPercent"
        }
        require(aspectRatioNumerator > 0.0) { "aspectRatioNumerator must be > 0" }
        require(aspectRatioDenominator > 0.0) { "aspectRatioDenominator must be > 0" }
        require(cornerRadiusDp >= 0) { "cornerRadiusDp must be >= 0" }
        require(borderWidthDp >= 0) { "borderWidthDp must be >= 0" }
    }

    internal companion object {
        const val DEFAULT_WIDTH_PERCENT = 35
        const val MIN_WIDTH_PERCENT = 10
        const val MAX_WIDTH_PERCENT = 90
        const val DEFAULT_ASPECT_RATIO_NUMERATOR = 16.0
        const val DEFAULT_ASPECT_RATIO_DENOMINATOR = 9.0
        const val DEFAULT_EDGE_MARGIN_PERCENT = 3
    }
}
