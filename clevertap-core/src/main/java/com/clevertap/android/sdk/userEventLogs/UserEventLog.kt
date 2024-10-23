package com.clevertap.android.sdk.userEventLogs

data class UserEventLog(
    val eventName: String,   // The name of the event
    val firstTs: Long,       // The timestamp of the first occurrence of the event
    val lastTs: Long,        // The timestamp of the last occurrence of the event
    val countOfEvents: Int,  // The number of times the event has occurred
    val deviceID: String     // The GUID/deviceID of the user where the event occurred
)
