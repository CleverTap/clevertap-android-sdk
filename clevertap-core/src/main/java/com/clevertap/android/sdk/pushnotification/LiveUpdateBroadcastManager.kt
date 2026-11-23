package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.Clock

/**
 * Owns the FCM topic subscription lifetime of broadcast Live Updates:
 * - broadcast START rendered → subscribe to `wzrk_topic` and record the run;
 * - End rendered / user Dismiss → unsubscribe and drop the run;
 * - before every subscribe → prune expired runs and evict the oldest past [softCap], since FCM caps
 *   an install at 2,000 topic subscriptions and every broadcast uses a new topic.
 *
 * Rendering and lifecycle events are untouched — those stay on the 1:1 path.
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

    /** Called after any Live Update is rendered (both render modes). */
    @WorkerThread
    fun onRendered(extras: Bundle) {
        when {
            LiveUpdateBroadcast.isEnd(extras) -> closeRun(extras, "end")
            LiveUpdateBroadcast.isStart(extras) && LiveUpdateBroadcast.isBroadcast(extras) -> openRun(extras)
        }
    }

    /** Called when the user swipes the Live Update away. */
    @WorkerThread
    fun onDismissed(extras: Bundle) {
        closeRun(extras, "dismiss")
    }

    /**
     * Unsubscribes every run whose expiry has passed. Returns the closed entries so the caller can
     * finish them (cancel the notification, raise Ended).
     */
    @WorkerThread
    fun pruneExpired(): List<LiveUpdateTopicEntry> {
        val now = clock.currentTimeSeconds()
        return store.all().filter { it.expiresAtSeconds <= now }.mapNotNull { close(it.topic, "expired") }
    }

    private fun openRun(extras: Bundle) {
        val topic = LiveUpdateBroadcast.topic(extras)
        if (topic == null) {
            logger.debug(accountId, "Live Update broadcast START without wzrk_topic; not subscribing.")
            return
        }
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

    /** Resolves the run from `wzrk_topic`, or — when a topic push omits it — from `wzrk_activityId`. */
    private fun closeRun(extras: Bundle, reason: String) {
        val topic = LiveUpdateBroadcast.topic(extras)
            ?: extras.getString(Constants.WZRK_LIVE_UPDATE_ID)?.let { store.findByActivityId(it)?.topic }
            ?: return
        close(topic, reason)
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

    private fun close(topic: String, reason: String): LiveUpdateTopicEntry? {
        val entry = store.remove(topic) ?: return null
        logger.verbose(accountId, "Live Update broadcast: closing $topic ($reason)")
        subscriber.unsubscribe(topic)
        return entry
    }

    companion object {
        /** Well under FCM's 2,000 per-install limit, leaving room for the app's own topics. */
        const val DEFAULT_SOFT_CAP = 500
    }
}
