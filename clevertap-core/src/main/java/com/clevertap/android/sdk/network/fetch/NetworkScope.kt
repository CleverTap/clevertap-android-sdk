package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Owns the lifetime of every V2 network coroutine in one SDK instance.
 *
 * Uses a [SupervisorJob] so one call failing does not cancel siblings.
 * The dispatcher is injected — default [Dispatchers.IO].
 *
 * Cancelled when the CleverTap instance is torn down. After [cancel],
 * no new coroutines may be launched on [coroutineScope].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class NetworkScope(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val job = SupervisorJob()

    val coroutineScope: CoroutineScope =
        CoroutineScope(job + dispatcher + CoroutineName("CT-Fetch"))

    fun cancel() {
        job.cancel()
    }
}
