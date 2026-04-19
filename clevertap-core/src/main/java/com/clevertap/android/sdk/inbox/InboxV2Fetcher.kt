package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.EndpointCall
import com.clevertap.android.sdk.network.fetch.FetchThrottle
import com.clevertap.android.sdk.response.InboxV2Response
import org.json.JSONObject

/**
 * Orchestrates a single Inbox V2 fetch. Sits above an [EndpointCall] and
 * below the public-facing bridge.
 *
 * Owns three concerns:
 *  - respecting the 5-minute throttle on user-initiated calls
 *  - a session-scoped "Disabled" flag set after the endpoint returns
 *    [CallResult.Disabled] (cleared on process restart)
 *  - handing successful responses to [InboxV2Response], which owns the
 *    inbox lock, controller-init-if-null and the UI callback
 *
 * Returns [CallResult.Success] holding [Unit] on success — the inbox cache
 * has already been updated and the callback has fired inside
 * [InboxV2Response].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxV2Fetcher(
    private val endpoint: EndpointCall<JSONObject>,
    private val throttle: FetchThrottle,
    private val inboxV2Response: InboxV2Response,
    private val logger: Logger
) {
    @Volatile
    private var disabledForSession: Boolean = false

    /**
     * @param respectThrottle true for pull-to-refresh and the public
     *   `fetchInbox()` API; false for app-launch and `onUserLogin`.
     */
    suspend fun fetch(respectThrottle: Boolean): CallResult<Unit> {
        if (disabledForSession) {
            logger.verbose("InboxV2", "disabled for session — skipping")
            return CallResult.Disabled
        }

        if (respectThrottle && throttle.shouldThrottle()) {
            logger.verbose("InboxV2", "throttled")
            return CallResult.Throttled
        }

        throttle.recordFetch()

        return when (val raw = endpoint.execute()) {
            is CallResult.Success -> {
                inboxV2Response.processResponse(raw.data)
                CallResult.Success(Unit)
            }
            CallResult.Disabled -> {
                disabledForSession = true
                CallResult.Disabled
            }
            is CallResult.HttpError -> raw
            is CallResult.NetworkFailure -> raw
            CallResult.Throttled -> CallResult.Throttled
        }
    }
}
