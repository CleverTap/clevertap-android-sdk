package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks
import java.util.concurrent.ExecutorService

/**
 * Groups all parameters needed by [FallbackImageLoader.load] into a single object.
 *
 * @param container       ViewGroup to add the fallback ImageView into
 * @param fallbackUrl     URL of the fallback image; if null/blank, fires onMediaError immediately
 * @param primaryUrl      The original media URL (used in error messages)
 * @param resourceProvider Cache/fetch provider
 * @param mediaExecutor   Background executor for network fetch
 * @param isReleased      Returns true if the renderer has been released (skips UI work)
 * @param callbacks       PIP callbacks for error reporting
 * @param errorContext    Human-readable context for error messages (e.g., "Image load failed")
 * @param onBitmapReady   Optional callback invoked with the loaded bitmap on main thread,
 *                        before the default ImageView is added. Return true to handle display
 *                        yourself (skips default ImageView creation), false to use the default.
 * @param onSuccess       Called when fallback image loaded successfully (cached or fetched)
 * @param onTotalFailure  Called when both primary and fallback failed — no media available
 */
internal data class FallbackLoadRequest(
    val container: ViewGroup,
    val fallbackUrl: String?,
    val primaryUrl: String,
    val resourceProvider: FileResourceProvider,
    val mediaExecutor: ExecutorService,
    val isReleased: () -> Boolean,
    val callbacks: PIPCallbacks?,
    val errorContext: String,
    val onBitmapReady: ((Bitmap) -> Boolean)? = null,
    val onSuccess: (() -> Unit)? = null,
    val onTotalFailure: (() -> Unit)? = null,
)

/**
 * Shared fallback image loading logic used by all media renderers.
 *
 * Cache-first pattern:
 * 1. Try [FileResourceProvider.cachedInAppImageV1] on main thread
 * 2. If miss, dispatch [FileResourceProvider.fetchInAppImageV1] on [mediaExecutor]
 * 3. On fetch success, create an [ImageView] and add to [container]
 * 4. On failure, fire [PIPCallbacks.onMediaError]
 */
internal object FallbackImageLoader {

    fun load(request: FallbackLoadRequest) {
        if (request.fallbackUrl.isNullOrBlank()) {
            request.callbacks?.onMediaError(request.primaryUrl, "${request.errorContext} and no fallback URL")
            request.onTotalFailure?.invoke()
            return
        }

        val fallbackUrl = request.fallbackUrl
        val cached = request.resourceProvider.cachedInAppImageV1(fallbackUrl)
        if (cached != null) {
            if (request.onBitmapReady?.invoke(cached) != true) {
                addFallbackImageView(request.container, cached)
            }
            request.onSuccess?.invoke()
        } else {
            request.mediaExecutor.execute {
                val fetched = request.resourceProvider.fetchInAppImageV1(fallbackUrl)
                request.container.post {
                    if (request.isReleased()) return@post
                    if (fetched != null) {
                        if (request.onBitmapReady?.invoke(fetched) != true) {
                            addFallbackImageView(request.container, fetched)
                        }
                        request.onSuccess?.invoke()
                    } else {
                        request.callbacks?.onMediaError(request.primaryUrl, "${request.errorContext} and fallback failed")
                        request.onTotalFailure?.invoke()
                    }
                }
            }
        }
    }

    private fun addFallbackImageView(container: ViewGroup, bitmap: Bitmap) {
        val iv = ImageView(container.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
        }
        container.addView(
            iv,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }
}
