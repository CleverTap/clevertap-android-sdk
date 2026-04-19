package com.clevertap.android.sdk.network.fetch

/**
 * A single-purpose network call — one request, one response. Implementations
 * own:
 *  - building their request body (event JSON + shared meta header)
 *  - calling the right CtApi method
 *  - mapping HTTP status + error codes into a [CallResult]
 *
 * Implementations must not throw for expected failures; return a [CallResult]
 * subtype instead. `CancellationException` from coroutines still propagates
 * naturally — don't swallow it.
 */
internal interface EndpointCall<T> {

    /**
     * Performs the call on whatever dispatcher the implementation chose
     * (typically [kotlinx.coroutines.Dispatchers.IO]).
     *
     * Safe to call from any coroutine context.
     */
    suspend fun execute(): CallResult<T>
}
