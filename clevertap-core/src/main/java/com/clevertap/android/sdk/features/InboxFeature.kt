package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.inbox.CTInboxController

/**
 * Inbox messaging feature
 * Manages inbox messages and notifications
 */
internal data class InboxFeature(
    val cTLockManager: CTLockManager
) {
    var ctInboxController: CTInboxController? = null
}
