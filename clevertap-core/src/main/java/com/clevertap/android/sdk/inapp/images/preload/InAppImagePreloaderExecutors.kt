package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class InAppImagePreloaderExecutors @JvmOverloads constructor(
    override val inAppImageProvider: InAppResourceProvider,
    override val logger: ILogger? = null,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader(),
    override val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) : InAppImagePreloaderStrategy {

    override fun preloadImages(urls: List<String>, successBlock: (url: String) -> Unit) {
        preloadAssets(urls, successBlock) { url ->
            inAppImageProvider.fetchInAppImage(url)
        }
    }

    override fun preloadGifs(urls: List<String>, successBlock: (url: String) -> Unit) {
        preloadAssets(urls, successBlock) { url ->
            inAppImageProvider.fetchInAppGif(url)
        }
    }

    private fun preloadAssets(
        urls: List<String>,
        successBlock: (url: String) -> Unit,
        assetBlock: (url: String) -> Any?
    ) {
        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute("tag") {
                val bitmap = assetBlock(url)
                if (bitmap != null) {
                    successBlock.invoke(url)
                }
            }
        }
    }

    override fun cleanup() {
        //executor?.shutdown
    }
}