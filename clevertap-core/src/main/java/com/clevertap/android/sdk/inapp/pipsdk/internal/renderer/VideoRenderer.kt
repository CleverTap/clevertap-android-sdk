package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
import android.widget.ImageView
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
 */
internal class VideoRenderer(
    private val resourceProvider: FileResourceProvider,
    private val mediaExecutor: ExecutorService,
) : MediaRenderer {

    private var session: PIPSession? = null
    private var wrapper: PIPVideoPlayerWrapper? = null
    private var containerRef: WeakReference<ViewGroup>? = null
    private var _isMuted = true
    private var _isPlaying = true
    //The flag is written on main thread (`release()`) and read on main thread (`view.post {}` callback), but the write could happen between the executor submitting the `post` and the `post` actually running. `@Volatile` ensures visibility across the handler message queue boundary.
    @Volatile private var released = false

    fun bindSession(s: PIPSession) {
        session = s
    }

    override fun attach(container: ViewGroup, config: PIPConfig, s: PIPSession) {
        released = false
        session = s
        containerRef = WeakReference(container)
        _isMuted = s.isMuted
        _isPlaying = s.isPlaying

        // Check video library availability
        if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.NONE) {
            loadFallbackAsImage(container, config, "No video library available")
            return
        }

        val existingWrapper = s.videoPlayerWrapper
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
            s.videoPlayerWrapper = w
            s.isMuted = true
            s.isPlaying = true

            container.addView(
                surface,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )

            // Start playback
            w.play()

            // Error listener for fallback
            setupErrorListener(container, config)
        }
    }

    override fun detachSurface() {
        val w = wrapper ?: return
        session?.playbackPositionMs = w.currentPositionMs
        session?.isPlaying = w.isPlaying
        session?.isMuted = w.isMuted
        w.detachSurface()
    }

    override fun rebindSurface(container: ViewGroup, s: PIPSession) {
        session = s
        containerRef = WeakReference(container)
        val w = s.videoPlayerWrapper ?: return
        wrapper = w
        val surface = w.rebindSurface(container.context) ?: return
        container.addView(
            surface,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        _isMuted = s.isMuted
        _isPlaying = s.isPlaying
    }

    override fun release() {
        released = true
        wrapper?.release()
        session?.videoPlayerWrapper = null
        wrapper = null
        containerRef = null
        session = null
    }

    override fun togglePlayPause() {
        val w = wrapper ?: return
        if (w.isPlaying) {
            w.softPause()
            _isPlaying = false
            session?.config?.callbacks?.onPlaybackPaused()
        } else {
            w.play()
            _isPlaying = true
            session?.config?.callbacks?.onPlaybackStarted()
        }
        session?.isPlaying = _isPlaying
    }

    override fun toggleMute() {
        val w = wrapper ?: return
        w.toggleMute()
        _isMuted = w.isMuted
        session?.isMuted = _isMuted
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
        val fb = config.fallbackUrl
        if (fb.isNullOrBlank()) {
            config.callbacks?.onMediaError(config.mediaUrl, errorMsg)
            return
        }
        container.removeAllViews()
        val iv = ImageView(container.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        container.addView(
            iv,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        val cached = resourceProvider.cachedInAppImageV1(fb)
        if (cached != null) {
            iv.setImageBitmap(cached)
        } else {
            mediaExecutor.execute {
                val fetched = resourceProvider.fetchInAppImageV1(fb)
                iv.post {
                    if (released) return@post
                    if (fetched != null) {
                        iv.setImageBitmap(fetched)
                    } else {
                        config.callbacks?.onMediaError(config.mediaUrl, "Video error and fallback failed")
                    }
                }
            }
        }
    }
}
