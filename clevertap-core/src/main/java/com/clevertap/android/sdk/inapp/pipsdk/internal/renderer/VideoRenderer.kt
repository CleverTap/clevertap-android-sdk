package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

/**
 * Renderer for [com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType.VIDEO].
 *
 * Delegates playback to [PIPVideoPlayerWrapper] which owns the player directly.
 * Uses [FileResourceProvider] for fallback image loading on video error.
 *
 * Reports state changes upward via [stateListener] instead of writing to
 * [PIPSession] directly — the coordinator wires the listener and handles
 * session updates.
 */
internal class VideoRenderer(
    private val resourceProvider: FileResourceProvider,
    private val mediaExecutor: ExecutorService,
) : MediaRenderer {

    private var wrapper: PIPVideoPlayerWrapper? = null
    private var containerRef: WeakReference<ViewGroup>? = null
    private var _isMuted = true
    private var _isPlaying = true

    /** Called when video playback fails and a static fallback image is shown instead.
     *  Used by [com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPMediaView] to notify
     *  parent views to hide video-specific controls. */
    var onFallbackToImage: (() -> Unit)? = null

    /** Listener for reporting state changes upward (player created/released, playback state). */
    var stateListener: RendererStateListener? = null

    override var onMediaReady: (() -> Unit)? = null
    override var onAllMediaFailed: (() -> Unit)? = null

    //The flag is written on main thread (`release()`) and read on main thread (`view.post {}` callback), but the write could happen between the executor submitting the `post` and the `post` actually running. `@Volatile` ensures visibility across the handler message queue boundary.
    @Volatile private var released = false

    override fun attach(container: ViewGroup, config: PIPConfig, session: PIPSession) {
        released = false
        containerRef = WeakReference(container)
        _isMuted = session.isMuted
        _isPlaying = session.isPlaying

        // PIPVideoPlayerWrapper uses Media3 APIs exclusively — old ExoPlayer is not supported.
        if (VideoLibChecker.mediaLibType != VideoLibraryIntegrated.MEDIA3) {
            loadFallbackAsImage(container, config, "Media3 video library not available")
            return
        }

        val existingWrapper = session.videoPlayerWrapper
        if (existingWrapper != null) {
            // Reuse existing wrapper (e.g. after expand/collapse)
            wrapper = existingWrapper
            val surface = existingWrapper.videoSurface()
            (surface.parent as? ViewGroup)?.removeView(surface)
            container.addView(
                surface,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            onMediaReady?.invoke()
        } else {
            // Create self-contained PIP video player
            val w = PIPVideoPlayerWrapper()
            w.initPlayer(container.context, config.mediaUrl)
            val surface = w.createSurface(container.context)

            w.setMuted(true)
            wrapper = w
            stateListener?.onPlayerCreated(w)
            stateListener?.onPlaybackStateChanged(isPlaying = true, isMuted = true, positionMs = 0L)

            // Sync UI controls when ExoPlayer's playing state changes on its own
            // (e.g., buffering → playing after network recovery)
            w.onPlayingChanged = { isPlaying ->
                _isPlaying = isPlaying
                stateListener?.onPlayPauseToggled(isPlaying)
            }

            container.addView(
                surface,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )

            w.play()
            w.notifyWhenFirstFrame {
                w.enableNetworkRetry()
                onMediaReady?.invoke()
            }

            // Error listener for fallback
            setupErrorListener(container, config)
        }
    }

    override fun detachSurface() {
        val w = wrapper ?: return
        stateListener?.onPlaybackStateChanged(
            isPlaying = w.isPlaying,
            isMuted = w.isMuted,
            positionMs = w.currentPositionMs,
        )
        w.detachSurface()
    }

    override fun rebindSurface(container: ViewGroup, session: PIPSession) {
        containerRef = WeakReference(container)
        val w = session.videoPlayerWrapper ?: return
        wrapper = w
        val surface = w.rebindSurface(container.context) ?: return
        container.addView(
            surface,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        _isMuted = session.isMuted
        _isPlaying = session.isPlaying

        // Re-register error listener with the new container so fallback loads
        // into the post-rotation view, not the old (detached) one.
        setupErrorListener(container, session.config)

        onMediaReady?.invoke()
    }

    override fun release() {
        released = true
        wrapper?.release()
        stateListener?.onPlayerReleased()
        wrapper = null
        containerRef = null
        stateListener = null
    }

    override fun togglePlayPause() {
        val w = wrapper ?: return
        if (w.isPlaying) {
            w.softPause()
            _isPlaying = false
        } else {
            w.play()
            _isPlaying = true
        }
        stateListener?.onPlayPauseToggled(_isPlaying)
    }

    override fun toggleMute() {
        val w = wrapper ?: return
        w.toggleMute()
        _isMuted = w.isMuted
        stateListener?.onMuteToggled(_isMuted)
    }

    override val currentPositionMs: Long get() = wrapper?.currentPositionMs ?: 0L
    override val isMuted: Boolean get() = _isMuted
    override val isPlaying: Boolean get() = wrapper?.isPlaying ?: _isPlaying

    private fun setupErrorListener(container: ViewGroup, config: PIPConfig) {
        wrapper?.setErrorListener { error ->
            container.post {
                // Why we check networkRetryEnabled here:
                //
                // When PIP is visible (networkRetryEnabled = true), we want network errors
                // to retry indefinitely. Our PIPLoadErrorPolicy handles this at the Loader
                // level — ExoPlayer retries and shows a buffering spinner automatically.
                //
                // However, there's a race condition: ExoPlayer's Loader captures the
                // retry count (getMinimumLoadableRetryCount) when a segment STARTS loading.
                // If a segment started before enableNetworkRetry() was called (before first
                // frame rendered), it captured the old limit (3 retries). After 3 failures,
                // the error escapes the Loader and reaches here via onPlayerError.
                //
                // Fix: when this happens (network error + PIP visible), we call
                // player.prepare() to restart loading. The NEW load captures the updated
                // retry limit (Int.MAX_VALUE), so ExoPlayer retries indefinitely from
                // this point. The user sees the buffering spinner until network returns.
                //
                // For pre-show (networkRetryEnabled = false) or non-network errors,
                // we use the existing fallback path: try fallback image → dismiss if both fail.
                if (isNetworkError(error) && wrapper?.isNetworkRetryEnabled == true) {
                    wrapper?.retryAfterNetworkError()
                } else {
                    loadFallbackAsImage(container, config, error.message ?: "Video playback error")
                }
            }
        }
    }

    /**
     * Returns true if the error is caused by a network connectivity issue
     * (DNS failure, timeout, connection refused, etc.).
     *
     * Returns false for HTTP status errors (404, 500) since those are permanent
     * server-side errors that won't be fixed by retrying.
     */
    private fun isNetworkError(error: PlaybackException): Boolean {
        val cause = error.cause
        return cause is HttpDataSource.HttpDataSourceException
                && cause !is HttpDataSource.InvalidResponseCodeException
    }

    private fun loadFallbackAsImage(container: ViewGroup, config: PIPConfig, errorMsg: String) {
        container.removeAllViews()
        // Release the failed video player — it's useless after an error and would otherwise
        // leak resources. Clearing the wrapper also lets PIPMediaView detect the fallback
        // state on rotation (videoPlayerWrapper == null means video failed).
        wrapper?.release()
        wrapper = null
        stateListener?.onPlayerReleased()
        onFallbackToImage?.invoke()
        FallbackImageLoader.load(
            FallbackLoadRequest(
                container = container,
                fallbackUrl = config.fallbackUrl,
                primaryUrl = config.mediaUrl,
                resourceProvider = resourceProvider,
                mediaExecutor = mediaExecutor,
                isReleased = { released },
                callbacks = config.callbacks,
                errorContext = errorMsg,
                onSuccess = { onMediaReady?.invoke() },
                onTotalFailure = { onAllMediaFailed?.invoke() },
            )
        )
    }
}
