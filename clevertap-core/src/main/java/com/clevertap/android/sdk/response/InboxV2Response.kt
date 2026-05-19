package com.clevertap.android.sdk.response

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.inbox.InboxMessageSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mirror of V1's `InboxResponse.java` for the Inbox V2 path.
 *
 * Sits between the fetcher / `/a1` decorator chain (network layer) and
 * `CTInboxController` (store). Owns exactly the same four guards V1 does:
 *  1. analytics-only early return
 *  2. response-key presence check
 *  3. broad try/catch around parse
 *  4. controller-init-if-null (so a response that races initialization
 *     isn't silently dropped)
 *
 * Two public entry points share the same parse/apply logic but carry a
 * different [InboxV2DeliverySource], which controls whether the
 * cross-device delete sweep runs inside `CTInboxController.processV2Response`:
 *  - [processResponse] (single-arg) — called directly by the V2 fetch
 *    pipeline (`InboxV2Fetcher`). Source = [InboxV2DeliverySource.FETCH].
 *  - the [CleverTapResponseDecorator] override — called by the `/a1`
 *    decorator chain for live-behaviour V2 messages. Source = [InboxV2DeliverySource.A1].
 *
 * Fires `inboxMessagesDidUpdate()` on a true update.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxV2Response(
    private val config: CleverTapInstanceConfig,
    ctLockManager: CTLockManager,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager,
    private val logger: ILogger = config.logger
) : CleverTapResponseDecorator() {
    private val inboxControllerLock: Any = ctLockManager.inboxControllerLock

    // ── /a1 decorator chain entry point ──────────────────────────────────────
    @WorkerThread
    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context?) {
        if (jsonBody == null) return
        processResponse(jsonBody, InboxV2DeliverySource.A1)
    }

    // ── Direct V2 fetch entry point ───────────────────────────────────────────
    @WorkerThread
    fun processResponse(response: JSONObject) {
        processResponse(response, InboxV2DeliverySource.FETCH)
    }

    // ── Shared parse + apply logic ────────────────────────────────────────────
    @WorkerThread
    private fun processResponse(response: JSONObject, source: InboxV2DeliverySource) {
        if (config.isAnalyticsOnly) {
            logger.verbose(config.accountId, "InboxV2: analytics-only mode — skipping response")
            return
        }

        logger.verbose(config.accountId, "InboxV2: Processing response (source=$source)")

        if (!response.has(Constants.INBOX_V2_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "InboxV2: response doesn't contain the v2 key")
            return
        }

        try {
            val messages = response.getJSONArray(Constants.INBOX_V2_JSON_RESPONSE_KEY)
            logger.verbose(config.accountId, "InboxV2: ${messages.length()} message(s) in inbox_notifs_v2")
            logger.verbose(config.accountId, "InboxV2: processing messages from server $messages ")
            _processInboxMessages(messages, source)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "InboxV2: Failed to parse response", t)
        }
    }

    @WorkerThread
    private fun _processInboxMessages(messages: JSONArray, source: InboxV2DeliverySource) {
        synchronized(inboxControllerLock) {
            if (controllerManager.ctInboxController == null) {
                controllerManager.initializeInboxSync()
            }
            val controller = controllerManager.ctInboxController ?: return
            val parsed = parseDaos(messages, controller.userId)
            val updated = controller.processV2Response(parsed, source)
            logger.verbose(config.accountId, "InboxV2: applied — updated=$updated")
            if (updated) {
                callbackManager._notifyInboxMessagesDidUpdate()
            }
        }
    }

    private fun parseDaos(messages: JSONArray, userId: String): List<CTMessageDAO> {
        val out = ArrayList<CTMessageDAO>(messages.length())
        for (i in 0 until messages.length()) {
            val obj = messages.optJSONObject(i) ?: continue
            val dao = CTMessageDAO.initWithJSON(obj, userId, InboxMessageSource.V2) ?: continue
            out.add(dao)
        }
        return out
    }
}
