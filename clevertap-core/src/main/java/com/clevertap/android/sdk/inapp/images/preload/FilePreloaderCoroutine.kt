package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        startedBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        preloadFinished: (urlDownloadStatus: Map<String, Boolean>) -> Unit
    ) {
        preloadAssets(
            urlMetas = urlMetas,
            successBlock = successBlock,
            failureBlock = failureBlock,
            startedBlock = startedBlock,
            preloadFinished = preloadFinished
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
        startedBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        preloadFinished: (urlDownloadStatus: Map<String, Boolean>) -> Unit = {},
        assetBlock: (meta: Pair<String, CtCacheType>) -> Any?
    ) {
        val job = scope.launch(handler) {
            val dowloadResults = mutableListOf<Deferred<Pair<String, Boolean>>>()
            urlMetas.forEach { meta: Pair<String, CtCacheType> ->
                val deferred: Deferred<Pair<String, Boolean>> = async {
                    logger?.verbose("started asset url fetch $meta")

                    startedBlock.invoke(meta)
                    val success: Boolean
                    val mils = measureTimeMillis {
                        val fetchInAppImage = assetBlock(meta)
                        if (fetchInAppImage != null) {
                            successBlock.invoke(meta)
                            success = true
                        } else {
                            failureBlock.invoke(meta)
                            success = false
                        }
                    }
                    logger?.verbose("finished asset url fetch $meta in $mils ms")

                    return@async meta.first to success
                }
                dowloadResults.add(deferred)
            }
            val pairs = dowloadResults.awaitAll() // waits for all downloads to finish
            preloadFinished.invoke(pairs.toMap())
        }
        jobs.add(job)
    }

    override fun cleanup() {
        jobs.forEach { job -> job.cancel() }
    }
}