package com.clevertap.android.sdk.pushnotification

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.Clock

/**
 * Drops stale Live Updates. FCM guarantees no delivery order, and topic fan-out plus direct sends
 * can interleave, so an older update may land after a newer one. iOS ActivityKit drops such updates
 * natively (by `timestamp`); Android has no equivalent, so the SDK does it here.
 *
 * Per `wzrk_activityId` it remembers the newest order value accepted — `wzrk_la_seq` when present,
 * else `timestamp` — and whether the activity has ended:
 * - an update/start older than the newest accepted is dropped (equal is allowed; `wzrk_pid` dedupe
 *   covers exact duplicates);
 * - once an `end` is accepted, every later non-end push for that activity is dropped;
 * - an `end` itself is never dropped (iOS parity).
 *
 * A push without `wzrk_activityId` or an order value is always accepted (no guard). Persisted so it
 * holds across process death; entries older than [RETENTION_SECONDS] are pruned on write.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LiveUpdateOrderGuard(
    private val prefs: SharedPreferences,
    private val clock: Clock
) {

    /** Returns whether the Live Update push should be rendered. */
    @WorkerThread
    fun accept(extras: Bundle): Boolean = synchronized(LOCK) {
        val activityId = extras.getString(Constants.WZRK_LIVE_UPDATE_ID)?.takeIf { it.isNotEmpty() }
            ?: return true
        val order = orderValue(extras) ?: return true
        val isEnd = Constants.WZRK_LIVE_UPDATE_EVENT_END.equals(
            extras.getString(Constants.WZRK_LIVE_UPDATE_EVENT), ignoreCase = true
        )
        val last = read(activityId)
        if (!isEnd && last != null && (last.ended || order < last.order)) {
            return false
        }
        write(
            activityId = activityId,
            entry = Entry(
                order = maxOf(order, last?.order ?: order),
                ended = isEnd || last?.ended == true,
                savedAtSeconds = clock.currentTimeSeconds()
            )
        )
        return true
    }

    private fun orderValue(extras: Bundle): Long? =
        extras.getString(Constants.WZRK_LIVE_UPDATE_SEQ)?.toLongOrNull()
            ?: extras.getString(Constants.LIVE_UPDATE_TIMESTAMP)?.toLongOrNull()

    private fun read(activityId: String): Entry? =
        prefs.getString(activityId, null)?.let { parse(it) }

    private fun write(activityId: String, entry: Entry) {
        val cutoff = clock.currentTimeSeconds() - RETENTION_SECONDS
        val editor = prefs.edit()
        prefs.all.forEach { (key, value) ->
            val saved = (value as? String)?.let { parse(it) }
            if (saved == null || saved.savedAtSeconds < cutoff) {
                editor.remove(key)
            }
        }
        editor.putString(activityId, "${entry.order}|${entry.ended}|${entry.savedAtSeconds}")
        editor.apply()
    }

    private fun parse(raw: String): Entry? {
        val parts = raw.split('|')
        if (parts.size != 3) {
            return null
        }
        return Entry(
            order = parts[0].toLongOrNull() ?: return null,
            ended = parts[1].toBoolean(),
            savedAtSeconds = parts[2].toLongOrNull() ?: return null
        )
    }

    private data class Entry(val order: Long, val ended: Boolean, val savedAtSeconds: Long)

    companion object {
        const val PREFS_NAMESPACE = "live_update_order"
        const val RETENTION_SECONDS = 48L * 60 * 60

        /** One prefs file is shared by every CleverTap instance in the process. */
        private val LOCK = Any()
    }
}
