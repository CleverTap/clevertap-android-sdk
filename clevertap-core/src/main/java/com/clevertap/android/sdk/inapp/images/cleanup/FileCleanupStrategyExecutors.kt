package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class FileCleanupStrategyExecutors @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader()
) : FileCleanupStrategy {

    companion object {
        private const val TAG = "InAppCleanupStrategyExecutors"
    }
    override fun clearInAppImagesAndGifsV1(urls: List<String>, successBlock: (url: String) -> Unit) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                fileResourceProvider.deleteImageMemoryV1(url)
                fileResourceProvider.deleteGifMemoryV1(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun clearFileAssetsV2(urls: List<String>, successBlock: (url: String) -> Unit) {
        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute("fileCleanupExecutor") {
                fileResourceProvider.deleteFileMemoryV2(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun stop() {
        // executor.stop()
    }
}