package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo

/**
 * Outcome of a single network call. Exhaustive sealed hierarchy — callers
 * pattern-match with `when` and the compiler guarantees every branch is
 * handled.
 *
 * Returned by every [EndpointCall]. Never throw for expected failures — use
 * the appropriate subtype. `CancellationException` still propagates naturally
 * from coroutines; don't swallow it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
sealed class CallResult<out T> {

    /** HTTP 200 + a parseable payload (or `Unit` for action-only calls). */
    data class Success<T>(val data: T) : CallResult<T>()

    /** Dropped by an endpoint-specific throttle before any network call. */
    object Throttled : CallResult<Nothing>()

    /**
     * Endpoint has been disabled for this session (e.g., a previous call
     * returned 403 and the orchestrator flipped a session flag).
     * Cleared on process restart.
     */
    object Disabled : CallResult<Nothing>()

    /** Non-2xx HTTP response. Body included for logging / diagnosis. */
    data class HttpError(val code: Int, val body: String?) : CallResult<Nothing>()

    /** Transport failure — IO, socket, parse, timeout. */
    data class NetworkFailure(val cause: Throwable) : CallResult<Nothing>()
}
