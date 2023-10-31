package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class InAppImagePreloader(
    private val inAppImageProvider: InAppImageProvider,
    val logger: ILogger? = null,
    val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) {

    private var job: Job? = null

    fun preloadImages(urls: List<String>) {

        val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
            logger?.verbose("Cancelled image pre fetch")
        }
        val scope = CoroutineScope(Dispatchers.IO)
        job = scope.launch(context = handler) {
            val list = mutableListOf<Deferred<Bitmap?>>()
            urls.chunked(
                config.parallelDownloads
            ).forEach { chunks ->
                logger?.verbose("Downloading image chunk with size ${chunks.size}")
                chunks.forEach { url ->
                    if (inAppImageProvider.isCached(url = url).not()) {
                        // start async download if not found in cache
                        val async: Deferred<Bitmap?> = async {
                            val bitmap = inAppImageProvider.fetchInAppImage<Bitmap>(url)
                            bitmap
                        }
                        list.add(async)
                    } else {
                        logger?.verbose("Found cached image for $url")
                    }
                }
                list.awaitAll()
            }
        }
    }

    fun cleanup() {
        job?.cancel()
    }
}

internal data class InAppImagePreloadConfig(
    val parallelDownloads: Int,
) {
    companion object {
        private const val DEFAULT_PARALLEL_DOWNLOAD = 4

        fun default() : InAppImagePreloadConfig = InAppImagePreloadConfig(
            parallelDownloads = DEFAULT_PARALLEL_DOWNLOAD
        )
    }
}