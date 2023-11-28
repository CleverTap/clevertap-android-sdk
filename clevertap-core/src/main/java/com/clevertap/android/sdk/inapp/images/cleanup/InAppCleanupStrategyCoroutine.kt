package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.utils.CtDefaultDispatchers
import com.clevertap.android.sdk.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class InAppCleanupStrategyCoroutine @JvmOverloads constructor(
    override val inAppResourceProvider: InAppResourceProvider,
    private val dispatchers: DispatcherProvider = CtDefaultDispatchers()
) : InAppCleanupStrategy {

    private var jobs: MutableList<Job> = mutableListOf()
    override fun clearAssets(urls: List<String>) {
        val job = CoroutineScope(dispatchers.io()).launch {

            val asyncTasks = mutableListOf<Deferred<Unit>>()
            for (url in urls) {
                val deferred: Deferred<Unit> = async {
                    inAppResourceProvider.deleteImage(url)
                    inAppResourceProvider.deleteGif(url)
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