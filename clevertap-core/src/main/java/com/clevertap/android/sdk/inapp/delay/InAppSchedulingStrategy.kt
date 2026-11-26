package com.clevertap.android.sdk.inapp.delay

import androidx.annotation.WorkerThread
import org.json.JSONObject
import androidx.annotation.RestrictTo

/**
 * Strategy interface for handling storage operations before/after scheduling
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface InAppSchedulingStrategy {
    /**
     * Prepare data before scheduling (e.g., save to DB)
     * @return true if preparation successful, false otherwise
     */
    @WorkerThread
    fun prepareForScheduling(inApps: List<JSONObject>): Boolean

    /**
     * Retrieve data after timer completes
     * @return JSONObject if found, null otherwise
     */
    @WorkerThread
    fun retrieveAfterTimer(id: String): JSONObject?

    /**
     * Cleanup after timer completes or cancelled
     */
    @WorkerThread
    fun clear(id: String)

    @WorkerThread
    fun clearAll()
}
