package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class FetchTrigger {
    USER_INITIATED, // pull-to-refresh, public fetchInbox() — throttle checked + recorded
    SYSTEM          // app-launch, onUserLogin — throttle bypassed, never recorded
}
