package com.clevertap.android.sdk.inapp.pipsdk

/**
 * Immutable configuration for a PIP session.
 *
 * Construct via [PIPConfig.builder] (Java-friendly) or the [Builder] directly.
 */
data class PIPConfig internal constructor(
    // Media
    val mediaUrl: String,
    val mediaType: PIPMediaType,
    val fallbackUrl: String? = null,            // static image shown if primary media fails

    // Sizing
    val widthPercent: Int = 35,
    val aspectRatioNumerator: Int = 16,
    val aspectRatioDenominator: Int = 9,

    // Positioning
    val initialPosition: PIPPosition = PIPPosition.BOTTOM_RIGHT,
    val horizontalEdgeMarginDp: Int = 16,       // left/right margin from screen edges
    val verticalEdgeMarginDp: Int = 16,         // top/bottom margin from screen edges

    // Animation
    val animation: PIPAnimation = PIPAnimation.DISSOLVE,

    // Controls
    val redirectUrl: String? = null,            // null = redirect button hidden
    val showCloseButton: Boolean = true,        // false = close button hidden everywhere

    // Compact view appearance
    val cornerRadiusDp: Int = 8,               // 0 = sharp corners
    val border: PIPBorderConfig = PIPBorderConfig(),

    // Controls visibility (from server "controls" JSON object)
    val dragEnabled: Boolean = true,
    val showPlayPauseButton: Boolean = true,
    val showMuteButton: Boolean = true,
    val showExpandCollapseButton: Boolean = true,

    // Callbacks
    val callbacks: PIPCallbacks? = null,
) {
    companion object {
        /** Primary entry point. Kotlin callers use named args; Java callers use Builder. */
        @JvmStatic
        fun builder(mediaUrl: String, mediaType: PIPMediaType): Builder =
            Builder(mediaUrl, mediaType)
    }

    class Builder(private val mediaUrl: String, private val mediaType: PIPMediaType) {
        private var fallbackUrl: String? = null
        private var widthPercent: Int = 35
        private var aspectRatioNumerator: Int = 16
        private var aspectRatioDenominator: Int = 9
        private var initialPosition: PIPPosition = PIPPosition.BOTTOM_RIGHT
        private var horizontalEdgeMarginDp: Int = 16
        private var verticalEdgeMarginDp: Int = 16
        private var animation: PIPAnimation = PIPAnimation.DISSOLVE
        private var redirectUrl: String? = null
        private var showCloseButton: Boolean = true
        private var cornerRadiusDp: Int = 8
        private var border: PIPBorderConfig = PIPBorderConfig()
        private var dragEnabled: Boolean = true
        private var showPlayPauseButton: Boolean = true
        private var showMuteButton: Boolean = true
        private var showExpandCollapseButton: Boolean = true
        private var callbacks: PIPCallbacks? = null

        fun fallbackUrl(url: String) = apply { fallbackUrl = url }
        fun widthPercent(value: Int) = apply { widthPercent = value }
        fun aspectRatio(numerator: Int, denominator: Int) = apply {
            aspectRatioNumerator = numerator
            aspectRatioDenominator = denominator
        }
        fun initialPosition(position: PIPPosition) = apply { initialPosition = position }
        fun horizontalEdgeMarginDp(dp: Int) = apply { horizontalEdgeMarginDp = dp }
        fun verticalEdgeMarginDp(dp: Int) = apply { verticalEdgeMarginDp = dp }
        fun animation(animation: PIPAnimation) = apply { this.animation = animation }
        fun redirectUrl(url: String) = apply { redirectUrl = url }
        fun showCloseButton(show: Boolean) = apply { showCloseButton = show }
        fun cornerRadiusDp(dp: Int) = apply { cornerRadiusDp = dp }
        fun border(border: PIPBorderConfig) = apply { this.border = border }
        fun dragEnabled(enabled: Boolean) = apply { dragEnabled = enabled }
        fun showPlayPauseButton(show: Boolean) = apply { showPlayPauseButton = show }
        fun showMuteButton(show: Boolean) = apply { showMuteButton = show }
        fun showExpandCollapseButton(show: Boolean) = apply { showExpandCollapseButton = show }
        fun callbacks(callbacks: PIPCallbacks) = apply { this.callbacks = callbacks }

        fun build(): PIPConfig {
            require(mediaUrl.isNotBlank()) { "mediaUrl must not be blank" }
            require(widthPercent in 10..90) { "widthPercent must be 10–90, got $widthPercent" }
            require(aspectRatioNumerator > 0) { "aspectRatioNumerator must be > 0" }
            require(aspectRatioDenominator > 0) { "aspectRatioDenominator must be > 0" }
            require(horizontalEdgeMarginDp >= 0) { "horizontalEdgeMarginDp must be >= 0" }
            require(verticalEdgeMarginDp >= 0) { "verticalEdgeMarginDp must be >= 0" }
            require(cornerRadiusDp >= 0) { "cornerRadiusDp must be >= 0" }
            require(border.widthDp >= 0) { "border.widthDp must be >= 0" }
            return PIPConfig(
                mediaUrl, mediaType, fallbackUrl, widthPercent, aspectRatioNumerator,
                aspectRatioDenominator, initialPosition, horizontalEdgeMarginDp,
                verticalEdgeMarginDp, animation, redirectUrl, showCloseButton,
                cornerRadiusDp, border, dragEnabled, showPlayPauseButton,
                showMuteButton, showExpandCollapseButton, callbacks,
            )
        }
    }
}
