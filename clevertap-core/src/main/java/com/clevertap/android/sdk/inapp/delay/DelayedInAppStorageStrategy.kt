package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.store.db.DelayedLegacyInAppStore
import org.json.JSONObject

/**
 * Storage strategy for delayed in-apps (requires database persistence)
 */
internal class DelayedInAppStorageStrategy(
    private val accountId: String,
    private val logger: Logger,
    internal var delayedLegacyInAppStore: DelayedLegacyInAppStore? = null
) : InAppSchedulingStrategy {

    override fun prepareForScheduling(inApps: List<JSONObject>): Boolean {
        if (delayedLegacyInAppStore == null) {
            logger.verbose(accountId, "DelayedLegacyInAppStore is null, cannot prepare")
            return false
        }

        // Save to database before scheduling
        return delayedLegacyInAppStore?.saveDelayedInAppsBatch(inApps) ?: false
    }

    override fun retrieveAfterTimer(id: String): JSONObject? {
        if (delayedLegacyInAppStore == null) {
            logger.verbose(accountId, "DelayedLegacyInAppStore is null, cannot retrieve")
            return null
        }

        // Retrieve from database
        return delayedLegacyInAppStore?.getDelayedInApp(id)
    }

    override fun clear(id: String) {
        delayedLegacyInAppStore?.removeDelayedInApp(id)
    }

    override fun clearAll() {
        delayedLegacyInAppStore?.removeAllDelayedInApps()
    }
}