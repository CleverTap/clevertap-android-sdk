package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo

/** Rendering mode chosen for a Live Update (live activity) push. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LiveActivityMode {
    /** SDK renders it (Core or Push Template) — chosen when the payload carries a `pt_id`. */
    SDK_RENDER,

    /** Client-supplied factory renders it — chosen when there is no `pt_id` and a factory is set. */
    FACTORY
}

/**
 * Pure routing decisions for a Live Update push, extracted from
 * `PushProviders._createNotification` so the Mode A / Mode B choice and the deterministic in-place
 * notification id are unit-testable without any Android I/O (no DB, NotificationManager, etc.).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveActivityRouter {

    /**
     * A `pt_id` in the payload (Mode B) means "SDK render this" and **wins over a registered
     * factory**. Only when there is no `pt_id` AND a factory is registered do we hand off to the
     * factory (Mode A). Otherwise the SDK renders (its Core/Template renderer).
     */
    @JvmStatic
    fun mode(isPtMode: Boolean, hasFactory: Boolean): LiveActivityMode =
        if (!isPtMode && hasFactory) LiveActivityMode.FACTORY else LiveActivityMode.SDK_RENDER

    /**
     * Deterministic, positive notification id derived from a stable key (the `wzrk_activityId`) so
     * successive updates for the same activity replace the same notification. Returns `null` when the
     * key is null/empty, letting the caller apply its own fallback.
     */
    @JvmStatic
    fun stableId(key: String?): Int? =
        key?.takeIf { it.isNotEmpty() }?.let { it.hashCode() and 0x7fffffff }
}
