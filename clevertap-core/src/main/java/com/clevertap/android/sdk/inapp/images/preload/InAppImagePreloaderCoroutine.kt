package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class InAppImagePreloaderCoroutine @JvmOverloads constructor(
    override val inAppImageProvider: InAppResourceProvider,
    override val logger: ILogger? = null,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers(),
    override val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) : InAppImagePreloaderStrategy {

    private val jobs: MutableList<Job> = mutableListOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun preloadImages(urls: List<String>) {

        val handler = CoroutineExceptionHandler { _, throwable ->
            logger?.verbose("Cancelled image pre fetch \n ${throwable.stackTrace}")
        }
        val scope = CoroutineScope(dispatchers.io().limitedParallelism(config.parallelDownloads))

        urls.forEach { url ->
            val job = scope.launch(handler) {
                inAppImageProvider.fetchInAppImage(url)
            }
            jobs.add(job)
        }
    }

    override fun cleanup() {
        jobs.map { job -> job.cancel() }
    }
}