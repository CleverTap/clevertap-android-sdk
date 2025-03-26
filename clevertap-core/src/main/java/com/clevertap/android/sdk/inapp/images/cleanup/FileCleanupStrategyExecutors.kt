package com.clevertap.android.sdk.inapp.images.cleanup

import android.content.Context
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.CTExecutors
/**
 * A file cleanup strategy that utilizes thread pools for asynchronous file deletion.
 *
 * This strategy clears file assets associated with provided URLs by executing deletion tasks on a background thread.
 *
 * @param context The context for accessing and managing file resources.
 * @param logger The logger
 * @param executor The executor providing a thread pool for asynchronous file deletion (defaults to a resource downloader executor with 8 threads).
 */
internal class FileCleanupStrategyExecutors @JvmOverloads constructor(
    override val context: Context,
    override val logger: ILogger,
    private val executor: CTExecutors = CTExecutorFactory.executorResourceDownloader()
) : FileCleanupStrategy {

    companion object {
        private const val TAG = "fileCleanupExecutor"
    }
    /** Clears file assets associated with the given URLs asynchronously using a thread pool.
     *
     * @param urls A list of URLs representing the file assets to be cleared.
     * @param successBlock A function to be executed for each URL that is successfully cleared.
     */
    override fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit) {
        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                FileResourceProvider.getInstance(context, logger).deleteData(url)
                successBlock.invoke(url)
            }
        }
    }
    /**
     * Stops any ongoing file cleanup operations initiated by this strategy.
     *
     * Currently, this method is not implemented.
     */
    override fun stop() {
        // executor.stop()
    }
}