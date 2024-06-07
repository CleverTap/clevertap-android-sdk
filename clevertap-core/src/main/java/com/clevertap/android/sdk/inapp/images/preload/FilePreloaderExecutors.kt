package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class FilePreloaderExecutors @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    override val logger: ILogger? = null,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader(),
    override val config: FilePreloadConfig = FilePreloadConfig.default()
) : FilePreloaderStrategy {

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
        for (url in urls) {
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