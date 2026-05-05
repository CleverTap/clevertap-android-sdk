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
): JSONObject {
    val event = JSONObject()
        .put("type", "event")
        .put("evtName", evtName)
        .put("evtData", evtData)
    stampEventMetadata(event, coreMetaData, clock, packageName)
    return event
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal fun stampEventMetadata(
    target: JSONObject,
    coreMetaData: CoreMetaData,
    clock: Clock,
    packageName: String
) {
    target.put("s", coreMetaData.currentSessionId)
    target.put("pg", CoreMetaData.getActivityCount())
    target.put("ep", clock.currentTimeSecondsInt())
    target.put("f", coreMetaData.isFirstSession)
    target.put("lsl", coreMetaData.lastSessionLength)
    target.put("pai", packageName)
    coreMetaData.screenName?.let { target.put("n", it) }
}
