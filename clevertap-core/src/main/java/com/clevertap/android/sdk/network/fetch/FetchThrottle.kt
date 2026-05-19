package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.utils.Clock

/**
 * In-memory per-instance throttle for a single network fetch feature.
 *
 * Each SDK instance owns its own [FetchThrottle]; independent instances
 * don't share state. The window resets on process death — subsequent
 * un-throttled callers (cold launch, onUserLogin) re-stamp the timestamp
 * via [recordFetch] before any throttled caller can read it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class FetchThrottle(
    private val windowMs: Long,
    private val clock: Clock = Clock.SYSTEM
) {
    @Volatile
    private var lastFetchMs: Long = 0L

    fun shouldThrottle(): Boolean =
        (clock.currentTimeMillis() - lastFetchMs) < windowMs

    fun recordFetch() {
        lastFetchMs = clock.currentTimeMillis()
    }

    fun reset() {
        lastFetchMs = 0L
    }
}
