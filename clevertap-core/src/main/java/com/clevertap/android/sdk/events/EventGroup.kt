package com.clevertap.android.sdk.events

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class EventGroup(
    @JvmField val httpResource: String,
    @JvmField val additionalPath: String
) {

    REGULAR("", ""),
    PUSH_NOTIFICATION_VIEWED("-spiky", ""),
    VARIABLES("", "/defineVars");
}
