package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import kotlin.math.min

/**
 * Self-contained video player for PIP in-app notifications.
 *
 * Owns the [ExoPlayer] and [PlayerView] directly (Media3 only), providing
 * non-destructive surface management needed for rotation survival.
 * PIP has its own overlay controls ([com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPControlsOverlay]),
 * so the PlayerView is created with `useController = false`.
 */
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
    private var playingChangedListener: Player.Listener? = null
    @Volatile private var networkRetryEnabled = false

    /** Called when ExoPlayer's isPlaying state changes (e.g., buffering → playing after
     *  network recovery). Used to sync UI controls that don't observe the player directly. */
    var onPlayingChanged: ((isPlaying: Boolean) -> Unit)? = null

    val isMuted: Boolean get() = _isMuted
    val isPlaying: Boolean get() = player?.isPlaying ?: _isPlaying

    val currentPositionMs: Long
        get() = player?.currentPosition ?: savedPositionMs

    /**
     * Creates and prepares the [ExoPlayer] with auto-detected media source.
     * Supports HLS, MP4, DASH, and other formats via [DefaultMediaSourceFactory].
     * Starts muted with repeat-one mode.
     */
    @OptIn(UnstableApi::class)
    fun initPlayer(context: Context, url: String) {
        if (player != null) return

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val trackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())

        val userAgent = Util.getUserAgent(context, context.packageName)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setTransferListener(bandwidthMeter.transferListener)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(PIPLoadErrorPolicy())

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
            }

        registerFirstFrameListener(player!!)
        registerPlayingChangedListener(player!!)
    }

    /**
     * Creates a bare [PlayerView] with no built-in controls and attaches the player.
     *
     * @return the [PlayerView] to add to the container.
     */
    @OptIn(UnstableApi::class)
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
        // Save playWhenReady (intent to play), not isPlaying (current state).
        // During buffering: isPlaying=false but playWhenReady=true. Saving isPlaying
        // would set playWhenReady=false after rotation, killing the retry + hiding the spinner.
        _isPlaying = p.playWhenReady
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
        registerFirstFrameListener(p)

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

    /** True after PIP becomes visible. Used by [VideoRenderer] to decide whether to
     *  retry on network error or fall through to the fallback image path. */
    val isNetworkRetryEnabled: Boolean get() = networkRetryEnabled

    /** Enable infinite retry for network errors. Call after first frame renders (PIP visible).
     *  Before this is called, default 3-retry behavior applies so pre-show errors still
     *  propagate to [onPlayerError] and trigger the fallback path. */
    fun enableNetworkRetry() { networkRetryEnabled = true }

    /**
     * Re-prepares the player after a network error that escaped the Loader's retry loop.
     *
     * Why this is needed:
     * ExoPlayer's Loader captures [getMinimumLoadableRetryCount] when a segment starts loading.
     * If that segment started BEFORE [enableNetworkRetry] was called, it has the old limit (3).
     * After 3 failures, the error propagates to onPlayerError even though we want infinite retry.
     *
     * Calling [ExoPlayer.prepare] resets the error state and starts a fresh load attempt.
     * The new load captures the UPDATED [getMinimumLoadableRetryCount] (Int.MAX_VALUE),
     * so from this point ExoPlayer retries indefinitely and shows the buffering spinner.
     */
    fun retryAfterNetworkError() {
        player?.prepare()
    }

    /**
     * Full cleanup — stops and releases the player.
     */
    fun release() {
        networkRetryEnabled = false
        synchronized(firstFrameLock) {
            firstFrameReady = false
            onFirstFrame = null
        }
        errorListener?.let { player?.removeListener(it) }
        errorListener = null
        playingChangedListener?.let { player?.removeListener(it) }
        playingChangedListener = null
        onPlayingChanged = null
        playerView?.player = null
        playerView = null
        player?.stop()
        player?.release()
        player = null
    }

    fun videoSurface(): View = checkNotNull(playerView) {
        "videoSurface() called but no PlayerView exists — was createSurface() called?"
    }

    /** Observes [Player.isPlaying] changes so UI controls (play/pause button) stay in sync
     *  when ExoPlayer transitions between states on its own (e.g., buffering → playing
     *  after network recovery). Without this, controls only update on user tap.
     *  Registered once in [initPlayer] — survives rotation since the player is not recreated. */
    private fun registerPlayingChangedListener(player: ExoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged?.invoke(isPlaying)
            }
        }
        playingChangedListener = listener
        player.addListener(listener)
    }

    /** Registers a one-shot [Player.Listener] that fires [onFirstFrame] when the first
     *  video frame is rendered. Used by both [initPlayer] (initial load) and
     *  [rebindSurface] (post-rotation) so [notifyWhenFirstFrame] works in both cases. */
    private fun registerFirstFrameListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val cb: (() -> Unit)?
                synchronized(firstFrameLock) {
                    firstFrameReady = true
                    cb = onFirstFrame
                    onFirstFrame = null
                }
                cb?.invoke()
                player.removeListener(this)
            }
        })
    }

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
    /**
     * Retries network connectivity errors indefinitely once [enableNetworkRetry] is called.
     * Before that, falls back to [DefaultLoadErrorHandlingPolicy] (3 retries, then error propagates).
     * HTTP status errors (404, 500) are never retried — they are permanent failures.
     */
    @OptIn(UnstableApi::class)
    private inner class PIPLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {
        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            val exception = loadErrorInfo.exception
            if (networkRetryEnabled
                && exception is HttpDataSource.HttpDataSourceException
                && exception !is HttpDataSource.InvalidResponseCodeException
            ) {
                return min(loadErrorInfo.errorCount * 2000L, 16_000L)
            }
            return super.getRetryDelayMsFor(loadErrorInfo)
        }

        // Loader.maybeThrowError(minRetryCount) bypasses getRetryDelayMsFor entirely —
        // if errorCount > minRetryCount, it throws directly to onPlayerError. Override to
        // prevent this when network retry is enabled. Safe for non-network errors because
        // getRetryDelayMsFor returns C.TIME_UNSET for them, making the Loader set fatalError
        // immediately — the retry count is never reached.
        override fun getMinimumLoadableRetryCount(dataType: Int): Int {
            return if (networkRetryEnabled) Int.MAX_VALUE else super.getMinimumLoadableRetryCount(dataType)
        }
    }
}
