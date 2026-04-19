package com.clevertap.android.sdk.response

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inbox.CTMessageDAO
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mirror of V1's `InboxResponse.java` for the Inbox V2 active-fetch path.
 *
 * Sits between the fetcher (network layer) and `CTInboxController` (store).
 * Owns exactly the same four guards V1 does:
 *  1. analytics-only early return
 *  2. response-key presence check
 *  3. broad try/catch around parse
 *  4. controller-init-if-null (so a response that races initialization
 *     isn't silently dropped)
 *
 * Fires `inboxMessagesDidUpdate()` on a true update. V2 deliberately
 * bypasses the shared `CleverTapResponseDecorator` chain.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxV2Response(
    private val config: CleverTapInstanceConfig,
    ctLockManager: CTLockManager,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager,
    private val logger: Logger = config.logger
) {
    private val inboxControllerLock: Any = ctLockManager.inboxControllerLock

    @WorkerThread
    fun processResponse(response: JSONObject) {
        if (config.isAnalyticsOnly) {
            logger.verbose(config.accountId, "InboxV2: analytics-only mode — skipping response")
            return
        }

        logger.verbose(config.accountId, "InboxV2: Processing response")

        if (!response.has(Constants.INBOX_V2_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "InboxV2: response doesn't contain the v2 key")
            return
        }

        try {
            val messages = response.getJSONArray(Constants.INBOX_V2_JSON_RESPONSE_KEY)
            _processInboxMessages(messages)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "InboxV2: Failed to parse response", t)
        }
    }

    @WorkerThread
    private fun _processInboxMessages(messages: JSONArray) {
        synchronized(inboxControllerLock) {
            if (controllerManager.ctInboxController == null) {
                controllerManager.initializeInbox()
            }
            val controller = controllerManager.ctInboxController ?: return
            val parsed = parseDaos(messages, controller.userId)
            val updated = controller.processV2Response(parsed)
            if (updated) {
                callbackManager._notifyInboxMessagesDidUpdate()
            }
        }
    }

    private fun parseDaos(messages: JSONArray, userId: String): List<CTMessageDAO> {
        val out = ArrayList<CTMessageDAO>(messages.length())
        for (i in 0 until messages.length()) {
            val obj = messages.optJSONObject(i) ?: continue
            val dao = CTMessageDAO.initWithJSON(obj, userId) ?: continue
            out.add(dao)
        }
        return out
    }
}
