package com.clevertap.android.sdk.inapp.images.cleanup

import android.content.Context
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
/**
 * A file cleanup strategy that utilizes Kotlin coroutines for asynchronous operations.
 *
 * This strategy clears file assets associated with provided URLs concurrently, leveraging coroutines for efficient
 * and non-blocking execution.
 *
 * @param context The context for accessing and managing file resources.
 * @param logger The logger
 * @param dispatchers The dispatcher provider for managing coroutine execution contexts (defaults to [CtDefaultDispatchers]).
 */
internal class FileCleanupStrategyCoroutine @JvmOverloads constructor(
    override val context: Context,
    override val logger: ILogger,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers()
) : FileCleanupStrategy {

    private var jobs: MutableList<Job> = mutableListOf()
    /**
     * Clears file assets associated with the given URLs asynchronously.
     *
     * @param urls A list of URLs representing the file assets to be cleared.
     * @param successBlock A function to be executed for each URL that is successfully cleared.
     */
    override fun clearFileAssets(
        urls: List<String>,
        successBlock: (url: String) -> Unit
    ) {
        val job = CoroutineScope(dispatchers.io()).launch {

            val asyncTasks = mutableListOf<Deferred<Unit>>()
            for (url in urls) {
                val deferred: Deferred<Unit> = async {
                    FileResourceProvider.getInstance(context, logger).deleteData(url)
                    successBlock.invoke(url)
                }
                asyncTasks.add(deferred)
            }
            asyncTasks.awaitAll()
        }
        jobs.add(job)
    }
    /**
     * Stops any ongoing file cleanup operations initiated by this strategy.
     */
    override fun stop() {
        jobs.forEach { job ->
            job.cancel()
        }
    }
}