package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
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

    //The flag is written on main thread (`release()`) and read on main thread (`view.post {}` callback), but the write could happen between the executor submitting the `post` and the `post` actually running. `@Volatile` ensures visibility across the handler message queue boundary.
    @Volatile private var released = false

    override fun attach(container: ViewGroup, config: PIPConfig, session: PIPSession) {
        released = false
        containerRef = WeakReference(container)
        _isMuted = session.isMuted
        _isPlaying = session.isPlaying

        // Check video library availability
        if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.NONE) {
            loadFallbackAsImage(container, config, "No video library available")
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
        } else {
            // Create self-contained PIP video player
            val w = PIPVideoPlayerWrapper()
            w.initPlayer(container.context, config.mediaUrl)
            val surface = w.createSurface(container.context)

            w.setMuted(true)
            wrapper = w
            stateListener?.onPlayerCreated(w)
            stateListener?.onPlaybackStateChanged(isPlaying = true, isMuted = true, positionMs = 0L)

            container.addView(
                surface,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )

            w.play()

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
                loadFallbackAsImage(container, config, error.message ?: "Video playback error")
            }
        }
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
            )
        )
    }
}
