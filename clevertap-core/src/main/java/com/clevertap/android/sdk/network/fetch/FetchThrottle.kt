package com.clevertap.android.sdk.network.fetch

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.utils.Clock

/**
 * Persistent per-account throttle for a single network fetch feature.
 *
 * The window is absolute across sessions — closing and reopening the app
 * doesn't reset it. Storage key is suffixed with the account id so
 * different accounts can't read each other's timestamps.
 *
 * Read/write is disk I/O — always call from a background context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class FetchThrottle(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val prefKey: String,
    private val windowMs: Long,
    private val clock: Clock = Clock.SYSTEM
) {
    @WorkerThread
    fun shouldThrottle(): Boolean = (clock.currentTimeMillis() - readLast()) < windowMs

    @WorkerThread
    fun recordFetch() {
        StorageHelper.putLong(context, keyed(), clock.currentTimeMillis())
    }

    @WorkerThread
    private fun readLast(): Long =
        StorageHelper.getLong(context, keyed(), 0L)

    private fun keyed(): String =
        StorageHelper.storageKeyWithSuffix(config.accountId, prefKey)
}
