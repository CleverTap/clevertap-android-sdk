package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.FallbackImageLoader
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.FallbackLoadRequest
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
        if (config.mediaContentDescription.isNotBlank()) {
            contentDescription = config.mediaContentDescription
        }
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

        if (reloadFallbackIfVideoFailed(session, resourceProvider, mediaExecutor)) return

        if (renderer == null) {
            renderer = createRenderer(session.config.mediaType, resourceProvider, mediaExecutor, session)
        }
        renderer?.rebindSurface(this, session)

        // Audio has no video frames → onRenderedFirstFrame() never fires → skip scrim
        if (session.config.mediaType != PIPMediaType.AUDIO) {
            attachVideoScrim(session.videoPlayerWrapper)
        }
    }

    /**
     * If video previously failed and fell back to a static image, reload the fallback
     * directly — there's no video player to rebind, so VideoRenderer.rebindSurface()
     * would return early and leave the container blank.
     *
     * Can't use [fellBackToImage] here because a new PIPMediaView is created on
     * rotation. Instead, infer from session: VIDEO type + no wrapper = video failed.
     *
     * @return true if fallback was reloaded (caller should return early)
     */
    private fun reloadFallbackIfVideoFailed(
        session: PIPSession,
        resourceProvider: FileResourceProvider,
        mediaExecutor: ExecutorService,
    ): Boolean {
        val isStreamMedia = session.config.mediaType == PIPMediaType.VIDEO
                || session.config.mediaType == PIPMediaType.AUDIO
        val videoFailedToImage = isStreamMedia && session.videoPlayerWrapper == null
        if (!videoFailedToImage) return false

        FallbackImageLoader.load(
            FallbackLoadRequest(
                container = this,
                fallbackUrl = session.config.fallbackUrl,
                primaryUrl = session.config.mediaUrl,
                resourceProvider = resourceProvider,
                mediaExecutor = mediaExecutor,
                isReleased = { false },
                callbacks = session.config.callbacks,
                errorContext = "Fallback reload after rotation",
            )
        )
        return true
    }

    /**
     * For video: overlay a black scrim on top of the PlayerView.
     *
     * SurfaceView renders *behind* window Views, so this scrim covers the black surface
     * while ExoPlayer seeks/decodes. Fades out once the first frame is rendered, with a
     * 3-second safety timeout.
     */
    private fun attachVideoScrim(wrapper: PIPVideoPlayerWrapper?) {
        wrapper ?: return

        val scrim = View(context).apply { setBackgroundColor(Color.BLACK) }
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val removeScrim = Runnable { removeView(scrim) }

        wrapper.notifyWhenFirstFrame {
            removeCallbacks(removeScrim)
            removeScrim.run()
        }
        // Use View.postDelayed() instead of handler?.postDelayed() because handler is null
        // before the view is attached. View queues callbacks internally until attachment.
        postDelayed(removeScrim, SCRIM_SAFETY_TIMEOUT_MS)
    }

    private companion object {
        const val SCRIM_SAFETY_TIMEOUT_MS = 3000L
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
        PIPMediaType.VIDEO, PIPMediaType.AUDIO -> VideoRenderer(
            resourceProvider, mediaExecutor, isAudio = mediaType == PIPMediaType.AUDIO,
        ).also { vr ->
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
