package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class FilePreloaderExecutors @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    override val logger: ILogger? = null,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader(),
    override val config: FilePreloadConfig = FilePreloadConfig.default()
) : FilePreloaderStrategy {

    override fun preloadFilesAndCache(
        urlMetas: List<Pair<String, CtCacheType>>,
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit
    ) {
        preloadAssets(urlMetas, successBlock, failureBlock) { urlMeta: Pair<String, CtCacheType> ->

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
        for (url in urlMetas) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute("tag") {
                val bitmap = assetBlock(url)
                if (bitmap != null) {
                    successBlock.invoke(url)
                } else {
                    failureBlock.invoke(url)
                }
            }
        }
    }

    override fun cleanup() {
        //executor?.shutdown
    }
}