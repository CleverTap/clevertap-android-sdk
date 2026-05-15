package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.EndpointCall
import com.clevertap.android.sdk.network.fetch.FetchThrottle
import com.clevertap.android.sdk.network.fetch.FetchTrigger
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
    private val logger: ILogger
) {
    @Volatile
    private var disabledForSession: Boolean = false

    /**
     * @param trigger [FetchTrigger.USER_INITIATED] for pull-to-refresh and the
     *   public `fetchInbox()` API — throttle is checked and recorded.
     *   [FetchTrigger.SYSTEM] for app-launch and `onUserLogin` — throttle is
     *   bypassed entirely and never recorded.
     */
    suspend fun fetch(trigger: FetchTrigger): CallResult<Unit> {
        if (disabledForSession) {
            logger.verbose("InboxV2", "disabled for session — skipping")
            return CallResult.Disabled
        }

        if (trigger == FetchTrigger.USER_INITIATED && throttle.shouldThrottle()) {
            logger.verbose("InboxV2", "throttled")
            return CallResult.Throttled
        }

        logger.verbose("InboxV2", "starting fetch (trigger=$trigger)")

        val raw = endpoint.execute()

        if (trigger == FetchTrigger.USER_INITIATED &&
                (raw is CallResult.Success || raw is CallResult.HttpError)) {
            throttle.recordFetch()
        }

        val result: CallResult<Unit> = when (raw) {
            is CallResult.Success -> {
                inboxV2Response.processResponse(raw.data)
                CallResult.Success(Unit)
            }
            CallResult.Disabled -> {
                disabledForSession = true
                logger.verbose("InboxV2", "session disabled — subsequent calls will short-circuit")
                CallResult.Disabled
            }
            is CallResult.HttpError -> raw
            is CallResult.NetworkFailure -> raw
            CallResult.Throttled -> CallResult.Throttled
        }
        logger.verbose("InboxV2", "fetch finished — ${result::class.simpleName}")
        return result
    }
}
