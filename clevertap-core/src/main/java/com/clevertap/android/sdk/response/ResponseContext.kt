package com.clevertap.android.sdk.response

/**
 * Immutable per-response context threaded through [CleverTapResponse.processResponse] so decorators
 * read request-scoped flags as a method argument instead of shared mutable fields. The fields were
 * set on singleton decorator instances right before each call, which raced when `/a1` and `/content`
 * responses were processed concurrently on different thread pools.
 *
 * - [isFullResponse] — whether this is a full `/a1` response (drives in-app asset cache eviction).
 * - [isUserSwitching] — a user switch is in progress (InAppResponse skips the display path).
 * - [source] — which endpoint the response came from ([CTResponseSource]).
 */
internal data class ResponseContext(
    val isFullResponse: Boolean,
    val isUserSwitching: Boolean,
    val source: CTResponseSource
) {
    companion object {
        /** Default context for direct processResponse calls: a non-full `/a1` response, no user switch. */
        @JvmField
        val DEFAULT = ResponseContext(
            isFullResponse = false,
            isUserSwitching = false,
            source = CTResponseSource.A1
        )
    }
}
