package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
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

    override fun preloadFilesAndCache(
        urlMetas: List<Pair<String, CtCacheType>>,
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit
    ) {
        preloadAssets(
            urlMetas = urlMetas,
            successBlock = successBlock,
            failureBlock = failureBlock
        ) { urlMeta: Pair<String, CtCacheType> ->

            val url = urlMeta.first

            when (urlMeta.second) {
                CtCacheType.IMAGE -> fileResourceProvider.fetchInAppImageV1(url)
                CtCacheType.GIF -> fileResourceProvider.fetchInAppGifV1(url)
                CtCacheType.FILES -> fileResourceProvider.fetchFile(url)
            }
        }
    }

    private fun preloadAssets(
        urlMetas: List<Pair<String, CtCacheType>>,
        successBlock: (meta: Pair<String, CtCacheType>) -> Unit,
        failureBlock: (meta: Pair<String, CtCacheType>) -> Unit = {},
        assetBlock: (meta: Pair<String, CtCacheType>) -> Any?
    ) {
        urlMetas.forEach { meta: Pair<String, CtCacheType> ->
            val job = scope.launch(handler) {
                logger?.verbose("started asset url fetch $meta")

                val mils = measureTimeMillis {
                    val fetchInAppImage = assetBlock(meta)
                    if (fetchInAppImage != null) {
                        successBlock.invoke(meta)
                    } else {
                        failureBlock.invoke(meta)
                    }
                }

                logger?.verbose("finished asset url fetch $meta in $mils ms")
            }
            jobs.add(job)
        }
    }

    override fun cleanup() {
        jobs.forEach { job -> job.cancel() }
    }
}