package com.clevertap.android.sdk.response

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONObject

internal open class InboxResponse(
    private val config: CleverTapInstanceConfig,
    ctLockManager: CTLockManager,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    private val inboxControllerLock: Any = ctLockManager.inboxControllerLock

    // inbox is reset and re-fetched for the new user after the switch
    override fun runsDuringUserSwitch(): Boolean = false

    //NotificationInbox
    @WorkerThread
    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing inbox messages"
            )
            return
        }

        logger.verbose(config.accountId, "Inbox: Processing response")

        // A null body throws here, as the Java original did (it dereferenced it unchecked).
        val response = checkNotNull(jsonBody) { "Inbox: response JSON is null" }
        if (!response.has(Constants.INBOX_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "Inbox: Response JSON object doesn't contain the inbox key")
            // process PushAmp response
            return
        }
        try {
            processInboxMessages(response.getJSONArray(Constants.INBOX_JSON_RESPONSE_KEY))
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "InboxResponse: Failed to parse response", t)
        }
    }

    // always call async
    @WorkerThread
    private fun processInboxMessages(messages: JSONArray) {
        synchronized(inboxControllerLock) {
            if (controllerManager.ctInboxController == null) {
                controllerManager.initializeInbox()
            }
            val updated = controllerManager.ctInboxController?.updateMessages(messages) ?: false
            if (updated) {
                callbackManager._notifyInboxMessagesDidUpdate()
            }
        }
    }
}
