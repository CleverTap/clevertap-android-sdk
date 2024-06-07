package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
internal class FilePreloaderCoroutine @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    override val logger: ILogger? = null,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers(),
    override val config: FilePreloadConfig = FilePreloadConfig.default()
) : FilePreloaderStrategy {

    private val jobs: MutableList<Job> = mutableListOf()
    private val handler = CoroutineExceptionHandler { _, throwable ->
        logger?.verbose("Cancelled image pre fetch \n ${throwable.stackTrace}")
    }
    private val scope = CoroutineScope(dispatchers.io().limitedParallelism(config.parallelDownloads))

    override fun preloadInAppImagesV1(urls: List<String>, successBlock: (url: String) -> Unit) {
        preloadAssets(urls, successBlock) { url ->
            fileResourceProvider.fetchInAppImageV1(url)
        }
    }

    override fun preloadInAppGifsV1(urls: List<String>, successBlock: (url: String) -> Unit) {
        preloadAssets(urls, successBlock) { url ->
            fileResourceProvider.fetchInAppGifV1(url)
        }
    }

    override fun preloadFiles(
        urls: List<String>,
        successBlock: (url: String) -> Unit,
        failureBlock: (url: String) -> Unit
    ) {
        preloadAssets(urls, successBlock,failureBlock) { url ->
            fileResourceProvider.fetchFile(url)
        }
    }

    private fun preloadAssets(
        urls: List<String>,
        successBlock: (url: String) -> Unit,
        failureBlock: (url: String) -> Unit = {},
        assetBlock: (url: String) -> Any?
    ) {
        urls.forEach { url ->
            val job = scope.launch(handler) {
                logger?.verbose("started asset url fetch $url")

                val mils = measureTimeMillis {
                    val fetchInAppImage = assetBlock(url)
                    if (fetchInAppImage != null) {
                        successBlock.invoke(url)
                    } else {
                        failureBlock.invoke(url)
                    }
                }

                logger?.verbose("finished asset url fetch $url in $mils ms")
            }
            jobs.add(job)
        }
    }

    override fun cleanup() {
        jobs.forEach { job -> job.cancel() }
    }
}