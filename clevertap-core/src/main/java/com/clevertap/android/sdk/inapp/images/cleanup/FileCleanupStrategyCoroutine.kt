package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class FileCleanupStrategyCoroutine @JvmOverloads constructor(
    override val fileResourceProvider: FileResourceProvider,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers()
) : FileCleanupStrategy {

    private var jobs: MutableList<Job> = mutableListOf()

    override fun clearFileAssets(
        urls: List<String>,
        successBlock: (url: String) -> Unit
    ) {
        val job = CoroutineScope(dispatchers.io()).launch {

            val asyncTasks = mutableListOf<Deferred<Unit>>()
            for (url in urls) {
                val deferred: Deferred<Unit> = async {
                    fileResourceProvider.deleteData(url)
                    successBlock.invoke(url)
                }
                asyncTasks.add(deferred)
            }
            asyncTasks.awaitAll()
        }
        jobs.add(job)
    }

    override fun stop() {
        jobs.forEach { job ->
            job.cancel()
        }
    }
}