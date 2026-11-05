package com.clevertap.android.sdk.pushnotification.preview

import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONException
import org.json.JSONObject

/**
 * Keeps trusted copies of Send Test previews (SDK-6282).
 *
 * [remember] is called wherever a push is seen at arrival, inside the app
 * process. [find] is called when a notification is tapped. The tap may come
 * from anywhere, including a web page's intent:// link, so the tapped intent's
 * content is used only to compute a lookup key and never rendered.
 *
 * Threading: [remember] may be called from any thread and returns at once.
 * [find] must run on the SDK's serial executor, the same one [remember] posts
 * to, so a row written at arrival is always visible by the time of the tap.
 *
 * One push is often seen twice (the SDK renders it, then raises its own
 * impression). The second sighting is stopped by a small recent-key guard
 * before any I/O. Example: key 3f9a... written at 10:00:00.120 by
 * _createNotification, seen again at 10:00:00.340 by pushNotificationViewedEvent,
 * skipped.
 */
internal class PushPreviewStore(
    private val context: Context,
    private val databaseManager: BaseDatabaseManager,
    private val executors: CTExecutors,
    private val clock: Clock,
    private val logger: ILogger,
    private val accountId: String,
) {

    /**
     * Keys written by this process, newest last, with the write time in millis.
     * Touched only on the serial executor, so no locking. Bounded by [RECENT_KEYS_MAX].
     * Process-local on purpose: another process has no guard and simply hits the
     * UNIQUE row in the database, which stays the source of truth.
     */
    private val recentlyRemembered = object : LinkedHashMap<String, Long>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > RECENT_KEYS_MAX
    }

    fun remember(extras: Bundle) {
        val payload = PushPreviewPayload.fromBundle(extras) ?: return
        // Capture now: the caller may mutate the bundle after we return.
        val collapseKey = extras.get(Constants.WZRK_COLLAPSE)?.toString()
        val ttlSeconds = extras.get(Constants.WZRK_TIME_TO_LIVE)?.toString()?.toLongOrNull()

        executors.postAsyncSafelyTask<Unit>().execute("rememberPushPreview") {
            val startedAt = clock.currentTimeMillis()
            val key = PushPreviewKey.of(payload)
            if (wasRecentlyRemembered(key, startedAt)) {
                logger.verbose(accountId, "Push preview already remembered, key=${key.take(KEY_LOG_CHARS)}")
                return@execute
            }
            val expiresAt = expiresAtSeconds(ttlSeconds, clock.currentTimeSeconds())
            databaseManager.loadDBAdapter(context)
                .savePushPreview(key, payload.toJson().toString(), expiresAt, collapseKey)
            recentlyRemembered[key] = startedAt
            val elapsed = clock.currentTimeMillis() - startedAt
            logger.verbose(
                accountId,
                "Push preview remembered in $elapsed ms, key=${key.take(KEY_LOG_CHARS)}, expires=$expiresAt"
            )
        }
    }

    /** The trusted copy as a bundle of preview fields only, or null when none was ever received. */
    @WorkerThread
    fun find(extras: Bundle): Bundle? {
        val payload = PushPreviewPayload.fromBundle(extras) ?: return null
        val key = PushPreviewKey.of(payload)
        val stored = databaseManager.loadDBAdapter(context)
            .findPushPreview(key, clock.currentTimeSeconds())
        return if (stored == null) {
            logger.debug(accountId, "Push preview ignored, no trusted copy for key=${key.take(KEY_LOG_CHARS)}")
            null
        } else {
            toTrustedBundle(stored, key)
        }
    }

    private fun toTrustedBundle(stored: String, key: String): Bundle? = try {
        PushPreviewPayload.fromJson(JSONObject(stored))?.toBundle()
    } catch (e: JSONException) {
        logger.debug(accountId, "Push preview ignored, stored copy unreadable for key=${key.take(KEY_LOG_CHARS)}", e)
        null
    }

    /** True when this process wrote [key] within [RECENT_KEYS_WINDOW_MILLIS] of [nowMillis]. */
    private fun wasRecentlyRemembered(key: String, nowMillis: Long): Boolean {
        val writtenAt = recentlyRemembered[key] ?: return false
        return nowMillis - writtenAt < RECENT_KEYS_WINDOW_MILLIS
    }

    companion object {
        /** Ceiling on how long a preview stays renderable. Matches the backend's S3 object lifetime. */
        const val MAX_TTL_SECONDS: Long = 24L * 60 * 60
        private const val KEY_LOG_CHARS = 8

        /** Enough for a Push Amplification batch; each entry is one 64 char string and a long. */
        private const val RECENT_KEYS_MAX = 16

        /**
         * Duplicate sightings of one arrival are milliseconds apart. Distinct sends are
         * seconds apart at least, and a repeat send after this window still refreshes
         * its expiry in the database.
         */
        private const val RECENT_KEYS_WINDOW_MILLIS = 10_000L

        /**
         * Backend proposes a lifetime through wzrk_ttl; the SDK caps it.
         * Example: wzrk_ttl ten years ahead still expires in 24 hours.
         */
        fun expiresAtSeconds(wzrkTtlSeconds: Long?, nowSeconds: Long): Long {
            val cap = nowSeconds + MAX_TTL_SECONDS
            return if (wzrkTtlSeconds != null && wzrkTtlSeconds > nowSeconds) minOf(wzrkTtlSeconds, cap) else cap
        }
    }
}
