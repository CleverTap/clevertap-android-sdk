package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.Clock

/**
 * Owns the FCM topic subscription lifetime of broadcast pushes. Driven only by the explicit
 * subscription keys — `wzrk_topic_action` (`subscribe` / `unsubscribe`) + `wzrk_topic` — never by the Live
 * Activity lifecycle (`wzrk_la_event`), which stays on the 1:1 path with its own events:
 * - `subscribe` → subscribe to `wzrk_topic` and record the run (+ its expiry and dismiss behaviour);
 * - `unsubscribe` → unsubscribe and drop the run;
 * - user swipe-away → unsubscribe only if the run opted in via `wzrk_topic_unsub_on_dismiss`;
 * - any other push for a run past its `dismissal_date` → unsubscribe and drop the push (catches
 *   late pushes without scheduled work; the optional expiry worker calls [expire] on time);
 * - before every subscribe → prune expired runs and evict the oldest past [softCap], since FCM caps
 *   an install at 2,000 topic subscriptions and every broadcast uses a new topic.
 *
 * Raises no events. A swiped-away run that did not opt in stays subscribed, so its next update
 * re-posts the notification.
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
     * Called on receipt of any CleverTap push that carries `wzrk_topic`, before rendering. Returns
     * whether the push should still be rendered: a pure control message (a `wzrk_topic_action`
     * without `wzrk_la`) never is.
     */
    @WorkerThread
    fun onReceived(extras: Bundle): Boolean {
        val topic = LiveUpdateBroadcast.topic(extras) ?: return true
        val isControl = when {
            LiveUpdateBroadcast.isSubscribe(extras) -> {
                openRun(topic, extras)
                true
            }
            LiveUpdateBroadcast.isUnsubscribe(extras) -> {
                // Unsubscribe even with no local record: the device may have been subscribed
                // server-side (batchAdd) or the record lost.
                close(topic, "unsubscribe", force = true)
                true
            }
            else -> {
                adoptIfUnknown(topic, extras)
                if (isExpired(topic)) {
                    logger.debug(accountId, "Broadcast topic $topic is past dismissal_date; dropping push.")
                    close(topic, "expired")
                    return false
                }
                false
            }
        }
        return !isControl || LiveUpdateRouter.isLiveUpdate(extras)
    }

    /** Unsubscribes a run at its `dismissal_date` (optional worker). No-op if already closed. */
    @WorkerThread
    fun expire(topic: String) {
        close(topic, "expired")
    }

    private fun isExpired(topic: String): Boolean =
        store.get(topic)?.let { it.expiresAtSeconds <= clock.currentTimeSeconds() } ?: false

    /** Called when the user swipes away a notification that carries `wzrk_topic`. */
    @WorkerThread
    fun onDismissed(extras: Bundle) {
        val topic = LiveUpdateBroadcast.topic(extras) ?: return
        if (store.get(topic)?.unsubscribeOnDismiss == true) {
            close(topic, "dismiss")
        }
    }

    /** Unsubscribes every run whose expiry has passed. */
    @WorkerThread
    fun pruneExpired() {
        val now = clock.currentTimeSeconds()
        store.all().filter { it.expiresAtSeconds <= now }.forEach { close(it.topic, "expired") }
    }

    private fun openRun(topic: String, extras: Bundle) {
        pruneExpired()
        evictOverCap()
        store.put(newEntry(topic, extras))
        subscriber.subscribe(topic)
    }

    /**
     * A topic push for a run this install has no record of (subscribed server-side, or the record
     * was lost) is adopted, so expiry and the dismiss flag still apply to it. No subscribe call: the
     * push arriving on the topic proves the install is already subscribed.
     */
    private fun adoptIfUnknown(topic: String, extras: Bundle) {
        if (store.get(topic) == null) {
            store.put(newEntry(topic, extras))
        }
    }

    private fun newEntry(topic: String, extras: Bundle): LiveUpdateTopicEntry {
        val now = clock.currentTimeSeconds()
        return LiveUpdateTopicEntry(
            topic = topic,
            activityId = extras.getString(Constants.WZRK_LIVE_UPDATE_ID),
            accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY),
            subscribedAtSeconds = now,
            expiresAtSeconds = LiveUpdateBroadcast.expiresAtSeconds(extras, now),
            unsubscribeOnDismiss = LiveUpdateBroadcast.unsubscribeOnDismiss(extras)
        )
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

    /** Closes a run once; [force] unsubscribes even when there is no record of it. */
    private fun close(topic: String, reason: String, force: Boolean = false) {
        if (store.remove(topic) == null && !force) {
            return
        }
        logger.verbose(accountId, "Live Update broadcast: closing $topic ($reason)")
        subscriber.unsubscribe(topic)
    }

    companion object {
        /** Well under FCM's 2,000 per-install limit, leaving room for the app's own topics. */
        const val DEFAULT_SOFT_CAP = 500
    }
}
