package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
import android.widget.ImageView
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import java.util.concurrent.ExecutorService

/**
 * Renderer for [com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType.IMAGE].
 *
 * Uses [FileResourceProvider] for cache-first bitmap loading:
 * 1. Try [FileResourceProvider.cachedInAppImageV1] on main thread (fast memory lookup)
 * 2. If miss, dispatch [FileResourceProvider.fetchInAppImageV1] on [mediaExecutor]
 * 3. On fetch success, post result to main thread
 * 4. On failure, try fallbackUrl via same two-step pattern
 */
internal class ImageRenderer(
    private val resourceProvider: FileResourceProvider,
    private val mediaExecutor: ExecutorService,
) : MediaRenderer {

    private var imageView: ImageView? = null
    private var config: PIPConfig? = null
    //The flag is written on main thread (`release()`) and read on main thread (`view.post {}` callback), but the write could happen between the executor submitting the `post` and the `post` actually running. `@Volatile` ensures visibility across the handler message queue boundary.
    @Volatile private var released = false

    override fun attach(container: ViewGroup, config: PIPConfig, session: PIPSession) {
        released = false
        this.config = config
        val iv = ImageView(container.context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            if (config.mediaContentDescription.isNotBlank()) {
                contentDescription = config.mediaContentDescription
            }
        }
        imageView = iv
        container.addView(
            iv,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        // Cache-first: try memory cache on main thread
        val cached = resourceProvider.cachedInAppImageV1(config.mediaUrl)
        if (cached != null) {
            iv.setImageBitmap(cached)
        } else {
            // Background fetch
            mediaExecutor.execute {
                val fetched = resourceProvider.fetchInAppImageV1(config.mediaUrl)
                iv.post {
                    if (released) return@post
                    if (fetched != null) {
                        iv.setImageBitmap(fetched)
                    } else {
                        loadFallback(iv, config)
                    }
                }
            }
        }
    }

    override fun detachSurface() = Unit

    override fun rebindSurface(container: ViewGroup, session: PIPSession) {
        val cfg = config ?: session.config
        attach(container, cfg, session)
    }

    override fun release() {
        released = true
        imageView?.setImageBitmap(null)
        imageView = null
        config = null
    }

    override fun setScaleType(scaleType: ImageView.ScaleType) {
        imageView?.scaleType = scaleType
    }

    override fun togglePlayPause() = Unit
    override fun toggleMute() = Unit
    override val currentPositionMs: Long = 0L
    override val isMuted: Boolean = false
    override val isPlaying: Boolean = false

    private fun loadFallback(iv: ImageView, config: PIPConfig) {
        val container = iv.parent as? ViewGroup ?: return
        FallbackImageLoader.load(
            FallbackLoadRequest(
                container = container,
                fallbackUrl = config.fallbackUrl,
                primaryUrl = config.mediaUrl,
                resourceProvider = resourceProvider,
                mediaExecutor = mediaExecutor,
                isReleased = { released },
                callbacks = config.callbacks,
                errorContext = "Image load failed",
                onBitmapReady = { bitmap -> iv.setImageBitmap(bitmap); true },
            )
        )
    }
}
