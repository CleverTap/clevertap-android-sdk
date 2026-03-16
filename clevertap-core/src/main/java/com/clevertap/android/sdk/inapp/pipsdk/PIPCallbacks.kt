package com.clevertap.android.sdk.inapp.pipsdk

/**
 * Callback interface for PIP lifecycle and media events.
 *
 * All methods have empty default implementations — Kotlin callers can override only
 * what they need. Java callers should extend [PIPCallbacksAdapter] instead.
 */
interface PIPCallbacks {
    fun onShow() {}
    fun onClose() {}
    fun onExpand() {}
    fun onCollapse() {}
    fun onRedirect(url: String) {}
    fun onPlaybackStarted() {}
    fun onPlaybackPaused() {}
    fun onMediaError(url: String, error: String) {}
}

/**
 * Abstract adapter for Java callers who don't want to implement every method of [PIPCallbacks].
 * Override only the methods you need.
 */
abstract class PIPCallbacksAdapter : PIPCallbacks {
    override fun onShow() {}
    override fun onClose() {}
    override fun onExpand() {}
    override fun onCollapse() {}
    override fun onRedirect(url: String) {}
    override fun onPlaybackStarted() {}
    override fun onPlaybackPaused() {}
    override fun onMediaError(url: String, error: String) {}
}
