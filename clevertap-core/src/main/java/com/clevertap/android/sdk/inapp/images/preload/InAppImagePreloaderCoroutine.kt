package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class InAppImagePreloaderCoroutine @JvmOverloads constructor(
        override val inAppImageProvider: InAppResourceProvider,
        override val logger: ILogger? = null,
        private val dispatchers: DispatcherProvider = CtDefaultDispatchers(),
        override val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) : InAppImagePreloaderStrategy {

    private var job: Job? = null

    override fun preloadImages(urls: List<String>) {

        val handler = CoroutineExceptionHandler { _, throwable ->
            logger?.verbose("Cancelled image pre fetch \n ${throwable.stackTrace}")
        }
        val scope = CoroutineScope(dispatchers.io())
        job = scope.launch(context = handler) {
            val list = mutableListOf<Deferred<Unit?>>()
            urls.chunked(
                config.parallelDownloads
            ).forEach { chunks ->
                logger?.verbose("Downloading image chunk with size ${chunks.size}")
                chunks.forEach { url ->
                    if (inAppImageProvider.isImageCached(url = url).not()) {
                        // start async download if not found in cache
                        val async: Deferred<Unit> = async {
                            inAppImageProvider.fetchInAppImage(url)
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

    override fun cleanup() {
        job?.cancel()
    }
}