package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
import android.widget.ImageView
import com.clevertap.android.sdk.gif.GifImageView
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import java.util.concurrent.ExecutorService

/**
 * Renderer for [com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType.GIF].
 *
 * Uses [GifImageView] from the shared media module for animated GIF playback,
 * and [FileResourceProvider] for cache-first byte[] loading.
 *
 * Cache-first pattern:
 * 1. Try [FileResourceProvider.cachedInAppGifV1] on main thread (fast memory lookup)
 * 2. If miss, dispatch [FileResourceProvider.fetchInAppGifV1] on [mediaExecutor]
 * 3. On fetch success, post result to main thread
 * 4. On failure, try fallbackUrl as static image via [FileResourceProvider]
 */
internal class GifRenderer(
    private val resourceProvider: FileResourceProvider,
    private val mediaExecutor: ExecutorService,
) : MediaRenderer {

    private var gifView: GifImageView? = null
    private var fallbackImageView: ImageView? = null
    private var config: PIPConfig? = null
    //The flag is written on main thread (`release()`) and read on main thread (`view.post {}` callback), but the write could happen between the executor submitting the `post` and the `post` actually running. `@Volatile` ensures visibility across the handler message queue boundary.
    @Volatile private var released = false

    override fun attach(container: ViewGroup, config: PIPConfig, session: PIPSession) {
        released = false
        this.config = config
        val gv = GifImageView(container.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        gifView = gv
        container.addView(
            gv,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        // Cache-first: try memory cache on main thread
        val cached = resourceProvider.cachedInAppGifV1(config.mediaUrl)
        if (cached != null) {
            gv.setBytes(cached)
            gv.startAnimation()
        } else {
            // Background fetch
            mediaExecutor.execute {
                val fetched = resourceProvider.fetchInAppGifV1(config.mediaUrl)
                gv.post {
                    if (released) return@post
                    if (fetched != null) {
                        gv.setBytes(fetched)
                        gv.startAnimation()
                    } else {
                        loadFallback(container, config)
                    }
                }
            }
        }
    }

    override fun detachSurface() = Unit // GIF is stateless — no position to save

    override fun rebindSurface(container: ViewGroup, session: PIPSession) {
        // GIF has no position state; just re-initialize
        val cfg = config ?: session.config
        attach(container, cfg, session)
    }

    override fun release() {
        released = true
        gifView?.stopAnimation()
        gifView?.clear()
        gifView = null
        fallbackImageView = null
        config = null
    }

    override fun togglePlayPause() = Unit
    override fun toggleMute() = Unit
    override val currentPositionMs: Long = 0L
    override val isMuted: Boolean = false
    override val isPlaying: Boolean = false

    private fun loadFallback(container: ViewGroup, config: PIPConfig) {
        FallbackImageLoader.load(
            container = container,
            fallbackUrl = config.fallbackUrl,
            primaryUrl = config.mediaUrl,
            resourceProvider = resourceProvider,
            mediaExecutor = mediaExecutor,
            isReleased = { released },
            callbacks = config.callbacks,
            errorContext = "GIF load failed",
            onBitmapReady = { _ ->
                // Remove GIF view before fallback ImageView is added by FallbackImageLoader
                gifView?.let { (it.parent as? ViewGroup)?.removeView(it) }
                gifView = null
                false // let FallbackImageLoader add the default ImageView
            },
        )
    }
}
