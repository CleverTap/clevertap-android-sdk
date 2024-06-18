package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FilePreloaderExecutors @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    override val logger: ILogger? = null,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader(),
    override val config: FilePreloadConfig = FilePreloadConfig.default()
) : FilePreloaderStrategy {

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
        startedBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        preloadFinished: (urlDownloadStatus: Map<String, Boolean>) -> Unit,
        assetBlock: (meta: Pair<String, CtCacheType>) -> Any?
    ) {
        val countDownLatch = CountDownLatch(urlMetas.size)
        val downloadStatus = urlMetas.map { meta ->
            meta.first to false
        }.associate {
            it
        }.toMutableMap()

        for (url in urlMetas) {
            val task = executor.ioTaskWithCallbackOnCurrentThread<Unit>()
            task.addOnSuccessListener { countDownLatch.countDown() }
            task.addOnFailureListener { countDownLatch.countDown() }
            task.execute("tag") {
                startedBlock.invoke(url)
                val bitmap = assetBlock(url)
                if (bitmap != null) {
                    downloadStatus[url.first] = true
                    successBlock.invoke(url)
                } else {
                    downloadStatus[url.first] = false
                    failureBlock.invoke(url)
                }
            }
        }
        try {
            // dont wait for more than 10 seconds to download.
            val success = countDownLatch.await(10, TimeUnit.SECONDS)
            if (success) {
                preloadFinished.invoke(downloadStatus)
            }
        } catch (e : InterruptedException) {
            //noop - not required for now
        }
    }

    override fun cleanup() {
        //executor?.shutdown
    }
}