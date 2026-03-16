package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.GifRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.ImageRenderer
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.MediaRenderer
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

    fun initialize(
        config: PIPConfig,
        session: PIPSession,
        resourceProvider: FileResourceProvider,
        mediaExecutor: ExecutorService,
    ) {
        removeAllViews()
        renderer = when (config.mediaType) {
            PIPMediaType.IMAGE -> ImageRenderer(resourceProvider, mediaExecutor)
            PIPMediaType.GIF -> GifRenderer(resourceProvider, mediaExecutor)
            PIPMediaType.VIDEO -> VideoRenderer(resourceProvider, mediaExecutor).also { it.bindSession(session) }
        }
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
        if (renderer == null) {
            renderer = when (session.config.mediaType) {
                PIPMediaType.IMAGE -> ImageRenderer(resourceProvider, mediaExecutor)
                PIPMediaType.GIF -> GifRenderer(resourceProvider, mediaExecutor)
                PIPMediaType.VIDEO -> VideoRenderer(resourceProvider, mediaExecutor).also { it.bindSession(session) }
            }
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
            wrapper.notifyWhenFirstFrame {
                scrim.animate().alpha(0f).setDuration(150).withEndAction { removeView(scrim) }.start()
            }
        }
    }

    fun release() = renderer?.release()
    fun togglePlayPause() = renderer?.togglePlayPause()
    fun toggleMute() = renderer?.toggleMute()

    val isVideoType: Boolean get() = renderer is VideoRenderer
    val isPlaying: Boolean get() = renderer?.isPlaying ?: false
    val isMuted: Boolean get() = renderer?.isMuted ?: false
}
