package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class InAppImagePreloader @JvmOverloads constructor(
    private val inAppImageProvider: InAppResourceProvider,
    private val logger: ILogger? = null,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers(),
    private val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) {

    private var job: Job? = null

    fun preloadImages(urls: List<String>) {

        val handler = CoroutineExceptionHandler { _, throwable ->
            logger?.verbose("Cancelled image pre fetch \n ${throwable.stackTrace}")
        }
        val scope = CoroutineScope(dispatchers.io())
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
                            val bitmap = inAppImageProvider.fetchInAppImage<Bitmap>(url, Bitmap::class.java)
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