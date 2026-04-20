package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.utils.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-key repeat-event suppression window.
 *
 * - Fires on the first `shouldSuppress(key)` call (returns false).
 * - Returns true — suppress — for any subsequent call with the same key
 *   within [windowMs].
 * - Every call updates the stored timestamp, so repeat bursts reset the
 *   window (sliding window).
 *
 * Thread-safe: uses `ConcurrentHashMap.put()`, a single atomic
 * check-and-update operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EventSuppressor @JvmOverloads constructor(
    private val windowMs: Long,
    private val clock: Clock = Clock.SYSTEM
) {
    private val lastSeen = ConcurrentHashMap<String, Long>()

    fun shouldSuppress(key: String): Boolean {
        val now = clock.currentTimeMillis()
        val prev = lastSeen.put(key, now)
        return prev != null && (now - prev) < windowMs
    }
}
