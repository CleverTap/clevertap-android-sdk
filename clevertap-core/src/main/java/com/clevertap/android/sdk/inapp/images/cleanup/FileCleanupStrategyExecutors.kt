package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors

internal class FileCleanupStrategyExecutors @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader()
) : FileCleanupStrategy {

    companion object {
        private const val TAG = "fileCleanupExecutor"
    }

    override fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit) {
        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                fileResourceProvider.deleteAsset(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun stop() {
        // executor.stop()
    }
}