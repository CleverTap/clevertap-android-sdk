package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.iterator
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage strategy for in-action in-apps (no database needed, metadata only)
 */
//TODO: check synchronization and map usage
internal class InActionStorageStrategy(
    private val logger: Logger,
    private val accountId: String
) : InAppSchedulingStrategy {

    // Cache metadata in memory
    private val inActionCache = ConcurrentHashMap<String, JSONObject>()

    override fun prepareForScheduling(inApps: JSONArray): Boolean {
        // Just cache in memory, no database needed
        inApps.iterator<JSONObject> {
            val id = it.optString(Constants.INAPP_ID_IN_PAYLOAD)
            if (id.isNotBlank()) {
                inActionCache[id] = it
            }
        }
        return true
    }

    override fun retrieveAfterTimer(id: String): JSONObject? {
        // Retrieve from memory cache
        return inActionCache[id]
    }

    override fun cleanup(id: String) {
        inActionCache.remove(id)
    }
}