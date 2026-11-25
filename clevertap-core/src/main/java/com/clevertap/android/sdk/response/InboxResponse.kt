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

    override fun runsDuringUserSwitch(): Boolean {
        return false // inbox is reset and re-fetched for the new user after the switch
    }

    //NotificationInbox
    @WorkerThread
    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        val response = jsonBody
        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing inbox messages"
            )
            return
        }

        logger.verbose(config.accountId, "Inbox: Processing response")

        if (!response!!.has(Constants.INBOX_JSON_RESPONSE_KEY)) {
            logger.verbose(config.accountId, "Inbox: Response JSON object doesn't contain the inbox key")
            // process PushAmp response
            return
        }
        try {
            _processInboxMessages(response.getJSONArray(Constants.INBOX_JSON_RESPONSE_KEY))
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "InboxResponse: Failed to parse response", t)
        }
    }

    // always call async
    @WorkerThread
    private fun _processInboxMessages(messages: JSONArray) {
        synchronized(inboxControllerLock) {
            if (controllerManager.ctInboxController == null) {
                controllerManager.initializeInbox()
            }
            if (controllerManager.ctInboxController != null) {
                val update = controllerManager.ctInboxController!!.updateMessages(messages)
                if (update) {
                    callbackManager._notifyInboxMessagesDidUpdate()
                }
            }
        }
    }
}
