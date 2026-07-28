package com.clevertap.android.sdk.inapp.pipsdk

/**
 * Callback interface for PIP lifecycle and media events.
 *
 * All methods have empty default implementations — override only what you need.
 */
interface PIPCallbacks {
    fun onShow() {}
    fun onClose() {}

    /** Called when the user taps the close (X) button specifically — as opposed to a CTA-triggered
     *  dismiss, a media-load failure, or Activity/session teardown, all of which surface via [onClose].
     *  Fires just before the corresponding [onClose] for the same dismissal. */
    fun onCloseButtonClick() {}
    fun onExpand() {}
    fun onCollapse() {}
    fun onAction() {}
    fun onPlaybackStarted() {}
    fun onPlaybackPaused() {}
    fun onMediaError(url: String, error: String) {}

    /** Called when PIP failed to show because all media URLs failed to load.
     *  PIP was never visible — no [onShow] or [onClose] will fire for this session. */
    fun onShowFailed() {}
}