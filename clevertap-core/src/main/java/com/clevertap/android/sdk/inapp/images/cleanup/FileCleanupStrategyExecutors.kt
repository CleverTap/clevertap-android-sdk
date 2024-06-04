package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class FileCleanupStrategyExecutors @JvmOverloads constructor(
    override val inAppResourceProvider: InAppResourceProvider,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader()
) : FileCleanupStrategy {

    companion object {
        private const val TAG = "InAppCleanupStrategyExecutors"
    }
    override fun clearInAppAssets(urls: List<String>, successBlock: (url: String) -> Unit) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                inAppResourceProvider.deleteImage(url)
                inAppResourceProvider.deleteGif(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit) {
        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute("fileCleanupExecutor") {
                inAppResourceProvider.deleteFile(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun stop() {
        // executor.stop()
    }
}