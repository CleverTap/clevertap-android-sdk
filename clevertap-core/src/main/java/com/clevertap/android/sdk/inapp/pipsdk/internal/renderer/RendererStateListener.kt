package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

/**
 * Callback interface for renderers to report state changes upward.
 * Keeps renderers decoupled from [com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession]
 * — they don't write session fields directly; instead, the coordinator
 * ([com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPMediaView]) handles it.
 */
internal interface RendererStateListener {
    /** A new video player was created. Session should store the reference. */
    fun onPlayerCreated(wrapper: PIPVideoPlayerWrapper)
    /** The video player was released. Session should clear its reference. */
    fun onPlayerReleased()
    /** Playback state snapshot — called before rotation detach so session can persist state. */
    fun onPlaybackStateChanged(isPlaying: Boolean, isMuted: Boolean, positionMs: Long)
    /** Play/pause state changed — fired on user toggle or player state transition
     *  (e.g., buffering → playing after network recovery). */
    fun onPlayPauseToggled(isPlaying: Boolean)
    /** User toggled mute. */
    fun onMuteToggled(isMuted: Boolean)
}
