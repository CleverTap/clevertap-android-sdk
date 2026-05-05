package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal fun buildInboxV2Event(
    evtName: String,
    evtData: JSONObject,
    coreMetaData: CoreMetaData,
    clock: Clock,
    packageName: String
): JSONObject = JSONObject().apply {
    put("type", "event")
    put("evtName", evtName)
    put("evtData", evtData)
    put("s", coreMetaData.currentSessionId)
    put("pg", CoreMetaData.getActivityCount())
    put("ep", clock.currentTimeSecondsInt())
    put("f", coreMetaData.isFirstSession)
    put("lsl", coreMetaData.lastSessionLength)
    put("pai", packageName)
    coreMetaData.screenName?.let { put("n", it) }
}
