package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.GifRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.ImageRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.MediaRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.PIPVideoPlayerWrapper
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.RendererStateListener
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.VideoRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import java.util.concurrent.ExecutorService

/**
 * FrameLayout that delegates all media rendering to a [MediaRenderer].
 *
 * This view is designed to be moved between [PIPCompactView] and [PIPExpandedView]
 * (removeView + addView) during expand/collapse transitions, keeping the same renderer
 * and player state intact.
 */
internal class PIPMediaView(context: Context) : FrameLayout(context) {

    private var renderer: MediaRenderer? = null
    private var session: PIPSession? = null
    private var fellBackToImage = false

    /** Called when a video renderer falls back to a static image after playback error.
     *  Parent views should hide video-specific controls (mute, play/pause) when this fires. */
    var onVideoFallback: (() -> Unit)? = null

    fun initialize(
        config: PIPConfig,
        session: PIPSession,
        resourceProvider: FileResourceProvider,
        mediaExecutor: ExecutorService,
    ) {
        removeAllViews()
        this.session = session
        fellBackToImage = false
        renderer = createRenderer(config.mediaType, resourceProvider, mediaExecutor, session)
        renderer?.attach(this, config, session)
    }

    /** Rotation: detach Surface from ExoPlayer. No-op for images. */
    fun detachSurface() = renderer?.detachSurface()

    /** Post-rotation: create new surface and re-bind existing player. */
    fun rebindSurface(
        session: PIPSession,
        resourceProvider: FileResourceProvider,
        mediaExecutor: ExecutorService,
    ) {
        removeAllViews()
        this.session = session
        if (renderer == null) {
            renderer = createRenderer(session.config.mediaType, resourceProvider, mediaExecutor, session)
        }
        renderer?.rebindSurface(this, session)

        // For video: overlay a black scrim on top of the PlayerView (SurfaceView renders *behind*
        // window Views, so this scrim covers the black surface while ExoPlayer seeks/decodes).
        // Fade the scrim out once the first frame is rendered.
        val wrapper = session.videoPlayerWrapper
        if (wrapper != null) {
            val scrim = View(context).apply {
                setBackgroundColor(Color.BLACK)
            }
            addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            val removeScrim = Runnable { removeView(scrim) }

            wrapper.notifyWhenFirstFrame {
                handler?.removeCallbacks(removeScrim)
                removeScrim.run()
            }
            // Safety net: remove scrim after 3s even if first frame never fires (e.g., video error)
            handler?.postDelayed(removeScrim, 3000L)
        }
    }

    fun release() = renderer?.release()
    fun onContainerChanged() = renderer?.onContainerChanged()
    fun togglePlayPause() = renderer?.togglePlayPause()
    fun toggleMute() = renderer?.toggleMute()
    fun setMediaScaleType(scaleType: ImageView.ScaleType) = renderer?.setScaleType(scaleType)

    /** Returns true only if the renderer is video AND has not fallen back to a static image. */
    val isVideoType: Boolean get() = renderer is VideoRenderer && !fellBackToImage
    val isPlaying: Boolean get() = renderer?.isPlaying ?: false
    val isMuted: Boolean get() = renderer?.isMuted ?: false

    /**
     * Creates the appropriate renderer for the given media type.
     * For VIDEO, wires the [RendererStateListener] to bridge state changes back to the session.
     */
    private fun createRenderer(
        mediaType: PIPMediaType,
        resourceProvider: FileResourceProvider,
        mediaExecutor: ExecutorService,
        session: PIPSession,
    ): MediaRenderer = when (mediaType) {
        PIPMediaType.IMAGE -> ImageRenderer(resourceProvider, mediaExecutor)
        PIPMediaType.GIF -> GifRenderer(resourceProvider, mediaExecutor)
        PIPMediaType.VIDEO -> VideoRenderer(resourceProvider, mediaExecutor).also { vr ->
            vr.onFallbackToImage = { fellBackToImage = true; onVideoFallback?.invoke() }
            vr.stateListener = object : RendererStateListener {
                override fun onPlayerCreated(wrapper: PIPVideoPlayerWrapper) {
                    session.videoPlayerWrapper = wrapper
                }

                override fun onPlayerReleased() {
                    session.videoPlayerWrapper = null
                }

                override fun onPlaybackStateChanged(isPlaying: Boolean, isMuted: Boolean, positionMs: Long) {
                    session.isPlaying = isPlaying
                    session.isMuted = isMuted
                    session.playbackPositionMs = positionMs
                }

                override fun onPlayPauseToggled(isPlaying: Boolean) {
                    session.isPlaying = isPlaying
                    if (isPlaying) session.config.callbacks?.onPlaybackStarted()
                    else session.config.callbacks?.onPlaybackPaused()
                }

                override fun onMuteToggled(isMuted: Boolean) {
                    session.isMuted = isMuted
                }
            }
        }
    }
}
