package com.clevertap.android.sdk

import androidx.annotation.RestrictTo

/**
 * Internal interface for communication between fragment and its respective activity when action buttons
 * are clicked via InApp/Inbox payload.
 *
 * Kept JVM- and Kotlin-public (but restricted) because public SDK activities implement it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface DidClickForHardPermissionListener {
    fun didClickForHardPermissionWithFallbackSettings(fallbackToSettings: Boolean)
    fun didCancelPermissionRequest()
}
