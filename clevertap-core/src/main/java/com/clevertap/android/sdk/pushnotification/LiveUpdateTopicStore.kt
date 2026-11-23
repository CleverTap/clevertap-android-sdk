package com.clevertap.android.sdk.pushnotification

import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import org.json.JSONObject

/** One open broadcast run: the topic this install is subscribed to and when it expires. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LiveUpdateTopicEntry(
    val topic: String,
    val activityId: String?,
    val accountId: String?,
    val subscribedAtSeconds: Long,
    val expiresAtSeconds: Long,
    /** A user swipe-away of the run's notification also unsubscribes (`wzrk_topic_unsub_on_dismiss`). */
    val unsubscribeOnDismiss: Boolean = false
)

/**
 * Persists the broadcast topics this install is subscribed to. FCM topics are per install (not per
 * CleverTap account), so this is one store per process, keyed by topic. It is the source of truth
 * for "is this run still open" — an entry is removed exactly once, by unsub, expiry or (when opted in) dismissal.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LiveUpdateTopicStore(private val prefs: SharedPreferences) {

    @Synchronized
    fun put(entry: LiveUpdateTopicEntry) {
        val json = JSONObject()
            .put(KEY_ACTIVITY_ID, entry.activityId)
            .put(KEY_ACCOUNT_ID, entry.accountId)
            .put(KEY_SUBSCRIBED_AT, entry.subscribedAtSeconds)
            .put(KEY_EXPIRES_AT, entry.expiresAtSeconds)
            .put(KEY_UNSUB_ON_DISMISS, entry.unsubscribeOnDismiss)
        prefs.edit().putString(entry.topic, json.toString()).apply()
    }

    /** Removes and returns the entry, or null if it was already removed. */
    @Synchronized
    fun remove(topic: String): LiveUpdateTopicEntry? {
        val entry = get(topic) ?: return null
        prefs.edit().remove(topic).apply()
        return entry
    }

    @Synchronized
    fun get(topic: String): LiveUpdateTopicEntry? =
        prefs.getString(topic, null)?.let { parse(topic, it) }

    @Synchronized
    fun all(): List<LiveUpdateTopicEntry> =
        prefs.all.mapNotNull { (topic, value) -> (value as? String)?.let { parse(topic, it) } }

    private fun parse(topic: String, raw: String): LiveUpdateTopicEntry? = try {
        val json = JSONObject(raw)
        LiveUpdateTopicEntry(
            topic = topic,
            activityId = json.optString(KEY_ACTIVITY_ID).takeIf { it.isNotEmpty() },
            accountId = json.optString(KEY_ACCOUNT_ID).takeIf { it.isNotEmpty() },
            subscribedAtSeconds = json.optLong(KEY_SUBSCRIBED_AT),
            expiresAtSeconds = json.optLong(KEY_EXPIRES_AT),
            unsubscribeOnDismiss = json.optBoolean(KEY_UNSUB_ON_DISMISS)
        )
    } catch (t: Throwable) {
        null
    }

    companion object {
        const val PREFS_NAMESPACE = "live_update_topics"
        private const val KEY_ACTIVITY_ID = "a"
        private const val KEY_ACCOUNT_ID = "acc"
        private const val KEY_SUBSCRIBED_AT = "s"
        private const val KEY_EXPIRES_AT = "e"
        private const val KEY_UNSUB_ON_DISMISS = "d"
    }
}
