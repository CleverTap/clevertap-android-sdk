package com.clevertap.android.sdk.events

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class EventGroup(
    val httpResource: String,
    val additionalPath: String
) {

    REGULAR("", ""),
    PUSH_NOTIFICATION_VIEWED("-spiky", ""),
    VARIABLES("", "/defineVars");
}
