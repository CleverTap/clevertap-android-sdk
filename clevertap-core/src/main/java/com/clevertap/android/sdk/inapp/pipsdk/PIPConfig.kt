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
    val animationConfig: PIPAnimationConfig = PIPAnimationConfig(),

    // Controls
    internal val action: com.clevertap.android.sdk.inapp.CTInAppAction? = null, // null = action button hidden
    val showCloseButton: Boolean = true,        // false = close button hidden everywhere

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
        private var animationConfig: PIPAnimationConfig = PIPAnimationConfig()
        private var action: com.clevertap.android.sdk.inapp.CTInAppAction? = null
        private var showCloseButton: Boolean = true
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
        fun animationConfig(config: PIPAnimationConfig) = apply { this.animationConfig = config }
        internal fun action(action: com.clevertap.android.sdk.inapp.CTInAppAction) = apply { this.action = action }
        fun showCloseButton(show: Boolean) = apply { showCloseButton = show }
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
            return PIPConfig(
                mediaUrl, mediaType, fallbackUrl, widthPercent, aspectRatioNumerator,
                aspectRatioDenominator, initialPosition, horizontalEdgeMarginDp,
                verticalEdgeMarginDp, animationConfig, action, showCloseButton,
                dragEnabled, showPlayPauseButton,
                showMuteButton, showExpandCollapseButton, callbacks,
            )
        }
    }
}
