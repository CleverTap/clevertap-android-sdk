package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.content.Context
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView

/**
 * Self-contained video player for PIP in-app notifications.
 *
 * Owns the [ExoPlayer] and [PlayerView] directly (Media3 only), providing
 * non-destructive surface management needed for rotation survival.
 * PIP has its own overlay controls ([com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPControlsOverlay]),
 * so the PlayerView is created with `useController = false`.
 */
@UnstableApi
internal class PIPVideoPlayerWrapper {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private var savedPositionMs: Long = 0L
    private var _isMuted: Boolean = true
    private var _isPlaying: Boolean = true

    // First-frame notification — used to delay compact-view visibility until video renders.
    // Guarded by [firstFrameLock] because onRenderedFirstFrame() fires on ExoPlayer's internal
    // thread while notifyWhenFirstFrame() is called from the main thread. A synchronized block
    // is used (not AtomicBoolean) because the check-then-act on both fields must be atomic.
    // ANR risk is zero — the lock body is only field reads/writes (~nanoseconds).
    private val firstFrameLock = Any()
    private var firstFrameReady = false
    private var onFirstFrame: (() -> Unit)? = null
    private var errorListener: Player.Listener? = null

    val isMuted: Boolean get() = _isMuted
    val isPlaying: Boolean get() = player?.isPlaying ?: _isPlaying

    val currentPositionMs: Long
        get() = player?.currentPosition ?: savedPositionMs

    /**
     * Creates and prepares the [ExoPlayer] with an HLS source.
     * Starts muted with repeat-one mode.
     */
    fun initPlayer(context: Context, url: String) {
        if (player != null) return

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val trackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())

        val userAgent = Util.getUserAgent(context, context.packageName)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setTransferListener(bandwidthMeter.transferListener)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setMediaSource(hlsMediaSource)
                prepare()
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
            }
    }

    /**
     * Creates a bare [PlayerView] with no built-in controls and attaches the player.
     *
     * @return the [PlayerView] to add to the container.
     */
    fun createSurface(context: Context): View {
        // Guard against calling createSurface() twice without releasing the first PlayerView.
        // If this happens, the old PlayerView reference is overwritten and can't be cleaned up
        // in release(), leading to two PlayerViews attached to one ExoPlayer (undefined behavior)
        // and a memory leak of the first PlayerView.
        check(playerView == null) { "createSurface() called twice — previous PlayerView not released" }
        val pv = PlayerView(context).apply {
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            player = this@PIPVideoPlayerWrapper.player
        }
        playerView = pv
        return pv
    }

    fun play() {
        player?.play()
        _isPlaying = true
    }

    fun softPause() {
        player?.pause()
        _isPlaying = false
    }

    fun toggleMute() {
        _isMuted = !_isMuted
        player?.volume = if (_isMuted) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted = muted
        player?.volume = if (muted) 0f else 1f
    }

    /**
     * Saves playback position and detaches the video surface WITHOUT destroying the player.
     * Called before view hierarchy removal during rotation.
     *
     * Uses [Player.setPlayWhenReady] instead of [Player.pause] to keep the decoder warm,
     * avoiding a full seek + decode cycle on rebind.
     */
    fun detachSurface() {
        synchronized(firstFrameLock) {
            firstFrameReady = false
            onFirstFrame = null
        }
        val p = player ?: return
        savedPositionMs = p.currentPosition
        _isPlaying = p.isPlaying
        playerView?.player = null
        playerView = null
        p.playWhenReady = false
    }

    /**
     * Creates a new [PlayerView] and reattaches the player.
     * Called after re-layout on the new Activity instance post-rotation.
     *
     * No seek is needed because [detachSurface] uses `playWhenReady = false`
     * which preserves the player's position without flushing the decoder.
     *
     * @return the new [PlayerView] to add to the container, or null if the player is gone.
     */
    fun rebindSurface(context: Context): View? {
        val p = player ?: return null

        val surface = createSurface(context)

        // Register one-shot first-frame listener before resuming so it is never missed
        synchronized(firstFrameLock) {
            firstFrameReady = false
        }
        p.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val cb: (() -> Unit)?
                synchronized(firstFrameLock) {
                    firstFrameReady = true
                    cb = onFirstFrame
                    onFirstFrame = null
                }
                cb?.invoke()
                p.removeListener(this)
            }
        })

        // Restore state
        p.playWhenReady = _isPlaying
        p.volume = if (_isMuted) 0f else 1f
        return surface
    }

    /**
     * Invokes [callback] as soon as the first video frame is rendered post-rotation.
     * If the first frame already arrived (race-safe), [callback] fires immediately.
     */
    fun notifyWhenFirstFrame(callback: () -> Unit) {
        synchronized(firstFrameLock) {
            if (firstFrameReady) {
                callback()
            } else {
                onFirstFrame = callback
            }
        }
    }

    /**
     * Full cleanup — stops and releases the player.
     */
    fun release() {
        synchronized(firstFrameLock) {
            firstFrameReady = false
            onFirstFrame = null
        }
        errorListener?.let { player?.removeListener(it) }
        errorListener = null
        playerView?.player = null
        playerView = null
        player?.stop()
        player?.release()
        player = null
    }

    fun videoSurface(): View = playerView!!

    /**
     * Registers a [Player.Listener] on the underlying player to receive error events.
     */
    fun setErrorListener(onError: (PlaybackException) -> Unit) {
        val p = player ?: return
        errorListener?.let { p.removeListener(it) }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error)
            }
        }
        errorListener = listener
        p.addListener(listener)
    }
}
