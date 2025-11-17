package com.clevertap.android.sdk.inapp.data

import org.json.JSONArray

data class PartitionedInApps(
    val immediateInApps: JSONArray,
    val delayedInApps: JSONArray
) {
    /**
     * Check if there are any immediate in-apps available
     */
    val hasImmediateInApps: Boolean get() = immediateInApps.length() > 0

    /**
     * Check if there are any delayed in-apps available
     */
    val hasDelayedInApps: Boolean get() = delayedInApps.length() > 0

    companion object {
        /**
         * Create empty PartitionedInApps instance
         */
        fun empty() = PartitionedInApps(JSONArray(), JSONArray())
    }
}