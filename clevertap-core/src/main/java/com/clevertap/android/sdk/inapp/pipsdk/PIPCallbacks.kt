package com.clevertap.android.sdk.inapp.pipsdk

/**
 * Callback interface for PIP lifecycle and media events.
 *
 * All methods have empty default implementations — override only what you need.
 */
interface PIPCallbacks {
    fun onShow() {}
    fun onClose() {}
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