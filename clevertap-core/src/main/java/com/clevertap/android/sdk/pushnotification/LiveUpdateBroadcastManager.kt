package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.Clock

/**
 * Owns the FCM topic subscription lifetime of broadcast Live Updates. It acts only on the control
 * messages, at receipt (independent of whether they render):
 * - broadcast START → subscribe to `wzrk_topic` and record the run;
 * - End carrying `wzrk_topic` → unsubscribe and drop the run;
 * - a topic push for a run past its `dismissal_date` → unsubscribe and drop the push (catches late
 *   pushes without any scheduled work; the optional expiry worker calls [expire] on time);
 * - before every subscribe → prune expired runs and evict the oldest past [softCap], since FCM caps
 *   an install at 2,000 topic subscriptions and every broadcast uses a new topic.
 *
 * Topic updates, user dismissal, rendering and lifecycle events are untouched — they stay on the
 * 1:1 path. A swiped-away run stays subscribed, so its next update re-posts the notification.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LiveUpdateBroadcastManager(
    private val store: LiveUpdateTopicStore,
    private val subscriber: LiveUpdateTopicSubscriber,
    private val clock: Clock,
    private val accountId: String,
    private val logger: ILogger,
    private val softCap: Int = DEFAULT_SOFT_CAP
) {

    /**
     * Called on receipt of a Live Update push that carries `wzrk_topic`, after the `wzrk_pid` dedupe
     * and before rendering. Returns whether the push should still be rendered.
     */
    @WorkerThread
    fun onReceived(extras: Bundle): Boolean {
        val topic = LiveUpdateBroadcast.topic(extras) ?: return true
        when {
            LiveUpdateBroadcast.isStart(extras) && LiveUpdateBroadcast.isBroadcast(extras) -> openRun(topic, extras)
            LiveUpdateBroadcast.isEnd(extras) -> close(topic, "end")
            isExpired(topic) -> {
                logger.debug(accountId, "Live Update broadcast: $topic is past dismissal_date; dropping push.")
                close(topic, "expired")
                return false
            }
        }
        return true
    }

    /** Unsubscribes a run at its `dismissal_date` (optional worker). No-op if End already closed it. */
    @WorkerThread
    fun expire(topic: String) {
        close(topic, "expired")
    }

    private fun isExpired(topic: String): Boolean =
        store.get(topic)?.let { it.expiresAtSeconds <= clock.currentTimeSeconds() } ?: false

    /** Unsubscribes every run whose expiry has passed. */
    @WorkerThread
    fun pruneExpired() {
        val now = clock.currentTimeSeconds()
        store.all().filter { it.expiresAtSeconds <= now }.forEach { close(it.topic, "expired") }
    }

    private fun openRun(topic: String, extras: Bundle) {
        pruneExpired()
        evictOverCap()
        val now = clock.currentTimeSeconds()
        store.put(
            LiveUpdateTopicEntry(
                topic = topic,
                activityId = extras.getString(Constants.WZRK_LIVE_UPDATE_ID),
                accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY),
                subscribedAtSeconds = now,
                expiresAtSeconds = LiveUpdateBroadcast.expiresAtSeconds(extras, now)
            )
        )
        subscriber.subscribe(topic)
    }

    private fun evictOverCap() {
        val entries = store.all()
        if (entries.size < softCap) {
            return
        }
        entries.sortedBy { it.subscribedAtSeconds }
            .take(entries.size - softCap + 1)
            .forEach { close(it.topic, "cap") }
    }

    private fun close(topic: String, reason: String) {
        store.remove(topic) ?: return
        logger.verbose(accountId, "Live Update broadcast: closing $topic ($reason)")
        subscriber.unsubscribe(topic)
    }

    companion object {
        /** Well under FCM's 2,000 per-install limit, leaving room for the app's own topics. */
        const val DEFAULT_SOFT_CAP = 500
    }
}
