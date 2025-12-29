package com.clevertap.android.sdk.inapp.data

import org.json.JSONArray

data class PartitionedInAppsWithInAction(
    val immediateInApps: JSONArray,
    val delayedInApps: JSONArray,
    val inActionInApps: JSONArray
) {
    fun hasImmediateInApps() = immediateInApps.length() > 0
    fun hasDelayedInApps() = delayedInApps.length() > 0
    fun hasInActionInApps() = inActionInApps.length() > 0

    companion object {
        fun empty() = PartitionedInAppsWithInAction(
            JSONArray(), JSONArray(), JSONArray()
        )
    }
}