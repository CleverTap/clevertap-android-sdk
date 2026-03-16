package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.content.Context
import android.view.View
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle

/**
 * Adapter wrapping [InAppVideoPlayerHandle] for PIPViewSDK's rotation-surviving lifecycle.
 *
 * The CT SDK's [InAppVideoPlayerHandle.pause] is destructive (calls stop + release),
 * which is unsuitable for rotation where we need to keep the player alive, detach the
 * surface, and rebind on the new Activity instance. This wrapper reaches through
 * [InAppVideoPlayerHandle.videoSurface] to access the underlying ExoPlayer/Media3 player
 * and performs non-destructive surface management.
 *
 * **Preferred long-term fix:** Add `detachSurface()` and `rebindSurface()` to
 * [InAppVideoPlayerHandle] interface in the shared module.
 */
internal class PIPVideoPlayerWrapper(private val handle: InAppVideoPlayerHandle) {

    private var savedPositionMs: Long = 0L
    private var _isMuted: Boolean = true
    private var _isPlaying: Boolean = true

    // First-frame notification — used to delay compact-view visibility until video renders.
    private var firstFrameReady = false
    private var onFirstFrame: (() -> Unit)? = null

    val isMuted: Boolean get() = _isMuted
    val isPlaying: Boolean get() = resolvePlayer()?.isPlaying ?: _isPlaying

    val currentPositionMs: Long
        get() = resolvePlayer()?.currentPosition ?: savedPositionMs

    fun play() {
        resolvePlayer()?.play() ?: handle.play()
        _isPlaying = true
    }

    fun softPause() {
        resolvePlayer()?.pause()
        _isPlaying = false
    }

    fun toggleMute() {
        _isMuted = !_isMuted
        resolvePlayer()?.volume = if (_isMuted) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted = muted
        resolvePlayer()?.volume = if (muted) 0f else 1f
    }

    /**
     * Saves playback position and detaches the video surface WITHOUT destroying the player.
     * Called before view hierarchy removal during rotation.
     */
    fun detachSurface() {
        firstFrameReady = false
        onFirstFrame = null
        val player = resolvePlayer() ?: return
        savedPositionMs = player.currentPosition
        _isPlaying = player.isPlaying
        player.clearVideoSurface()
        player.pause()
    }

    /**
     * Creates a new [PlayerView], reattaches the player, and seeks to the saved position.
     * Called after re-layout on the new Activity instance post-rotation.
     *
     * @return the new [PlayerView] to add to the container, or null if the player is gone.
     */
    fun rebindSurface(context: Context): View? {
        val player = resolvePlayer() ?: return null
        player.seekTo(savedPositionMs)
        // Create a fresh PlayerView and attach the existing player
        handle.initPlayerView(context, false)
        val surface = handle.videoSurface()
        // Register one-shot first-frame listener before play() so it is never missed
        firstFrameReady = false
        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameReady = true
                onFirstFrame?.invoke()
                onFirstFrame = null
                player.removeListener(this)
            }
        })
        // Restore state
        if (_isPlaying) player.play() else player.pause()
        player.volume = if (_isMuted) 0f else 1f
        return surface
    }

    /**
     * Invokes [callback] as soon as the first video frame is rendered post-rotation.
     * If the first frame already arrived (race-safe), [callback] fires immediately.
     */
    fun notifyWhenFirstFrame(callback: () -> Unit) {
        if (firstFrameReady) {
            callback()
        } else {
            onFirstFrame = callback
        }
    }

    /**
     * Full cleanup — delegates to the handle's destructive [InAppVideoPlayerHandle.pause]
     * for final resource release.
     */
    fun release() {
        firstFrameReady = false
        onFirstFrame = null
        handle.pause()
    }

    fun videoSurface(): View = handle.videoSurface()

    /**
     * Registers a [Player.Listener] on the underlying player to receive error events.
     * The [onError] callback is invoked on the main thread when a playback error occurs.
     */
    fun setErrorListener(onError: (PlaybackException) -> Unit) {
        val player = resolvePlayer() ?: return
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error)
            }
        })
    }

    /**
     * Resolves the underlying [Player] by reaching through the handle's video surface.
     * Falls back to null if the surface is not a [PlayerView] or player is not set.
     */
    private fun resolvePlayer(): Player? {
        return try {
            (handle.videoSurface() as? PlayerView)?.player
        } catch (_: IllegalStateException) {
            null
        }
    }
}
