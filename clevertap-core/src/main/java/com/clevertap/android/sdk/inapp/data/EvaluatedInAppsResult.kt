package com.clevertap.android.sdk.inapp.data

import org.json.JSONObject

/**
 * Represents the result of evaluating in-app notifications for an event.
 *
 * This data class encapsulates the three categories of evaluated in-app notifications:
 * - Immediate client-side in-apps ready for direct display
 * - Delayed client-side in-apps to be scheduled
 * - Server-side in-action in-apps metadata
 *
 * @property immediateClientSideInApps In-apps without delay (or delay = 0) ready for immediate display
 * @property delayedClientSideInApps In-apps with delayAfterTrigger > 0 and <= 1200 seconds to be scheduled
 * @property serverSideInActionInApps Server-side in-action metadata for scheduling
 */
data class EvaluatedInAppsResult(
    val immediateClientSideInApps: List<JSONObject>,
    val delayedClientSideInApps: List<JSONObject>,
    val serverSideInActionInApps: List<JSONObject>
)

/**
 * Represents the result of evaluating client-side in-app notifications for App Launched event.
 *
 * This is a simpler result containing only immediate and delayed client-side in-apps,
 * without server-side in-action metadata (which is handled separately for app launch).
 *
 * @property immediateInApps In-apps without delay (or delay = 0) ready for immediate display
 * @property delayedInApps In-apps with delayAfterTrigger > 0 and <= 1200 seconds to be scheduled
 */
data class ClientSideInAppsResult(
    val immediateInApps: List<JSONObject>,
    val delayedInApps: List<JSONObject>
)