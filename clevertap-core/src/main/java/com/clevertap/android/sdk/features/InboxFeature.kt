package com.clevertap.android.sdk.features

import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CTInboxListener
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.response.InboxResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This class encapsulates the logic for handling the App Inbox feature of the CleverTap SDK.
 * It is responsible for processing inbox-related data from API responses, managing the lifecycle
 * of inbox messages, and notifying listeners about updates.
 *
 * The class interacts with various components:
 * - [InboxResponse] to parse and process inbox messages from server responses.
 * - [CTInboxController] to manage the state and data of the inbox.
 * - [CTInboxListener] to provide callbacks to the user about inbox initialization and updates.
 * - [CoreContract] to access core SDK functionalities like configuration and logging.
 *
 * It implements [CleverTapFeature] to integrate into the SDK's feature handling mechanism and
 * [InboxLiveCallbacks] to provide internal notifications about inbox state changes.
 *
 * @property mainPost A higher-order function to execute a given function on the main UI thread.
 *                    Defaults to using [Utils.runOnUiThread].
 */
internal class InboxFeature(
    val mainPost: (() -> Unit) -> Unit = { func -> Utils.runOnUiThread { func } }
) : CleverTapFeature, InboxLiveCallbacks {
    
    var ctInboxController: CTInboxController? = null
    var inboxListener: CTInboxListener? = null
    lateinit var coreContract: CoreContract

    private val logger
        get() = coreContract.logger()

    private val accountId
        get() = coreContract.config().accountId

    // Lazy-initialized Inbox dependencies (initialized after coreContract is set)
    val inboxResponse: InboxResponse by lazy {
        InboxResponse(
            coreContract.config().accountId,
            coreContract.logger(),
            coreContract.ctLockManager()
        )
    }

    override fun _notifyInboxMessagesDidUpdate() {
        inboxListener?.let {
            mainPost(it::inboxMessagesDidUpdate)
        }
    }

    override fun _notifyInboxInitialized() {
        inboxListener?.inboxDidInitialize()
    }

    override fun handleApiData(response: JSONObject) {
        if (coreContract.config().isAnalyticsOnly) {
            logger.verbose(
                accountId,
                "CleverTap instance is configured to analytics only, not processing inbox messages"
            )
            return
        }
        inboxResponse.processResponse(response)
    }

    @WorkerThread
    fun handleSendTestInbox(extras: Bundle) {
        extras.getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)?.let { notifPayload ->
            try {
                Logger.v("Received inbox via push payload: $notifPayload")

                val notificationJson = JSONObject(notifPayload).apply {
                    // Add an identifier, consider if this is the best approach for IDs
                    put("_id", coreContract.clock().currentTimeSeconds())
                }

                val jsonResponse = JSONObject().apply {
                    put(Constants.INBOX_JSON_RESPONSE_KEY, JSONArray().put(notificationJson))
                }

                handleApiData(jsonResponse)

            } catch (e: JSONException) {
                Logger.v("Failed to process inbox message from push notification payload", e)
            }
        }
    }

    /**
     * Phase 1: Accessor method moved from CoreState
     * Returns the CTInboxController instance
     */
    fun getCTInboxController(): CTInboxController? {
        return ctInboxController
    }

    /**
     * Phase 2: Initialization method moved from CoreState
     * Initializes the inbox controller (must be called on worker thread)
     */
    @WorkerThread
    fun initialize() {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().debug(
                coreContract.config().accountId,
                "Instance is analytics only, not initializing Notification Inbox"
            )
            return
        }
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                _notifyInboxInitialized()
                return
            }
            val deviceId = coreContract.deviceInfo().getDeviceID()
            if (deviceId != null) {
                ctInboxController = CTInboxController(
                    deviceId,
                    coreContract.database().loadDBAdapter(coreContract.context()),
                    coreContract.ctLockManager(),
                    com.clevertap.android.sdk.video.VideoLibChecker.haveVideoPlayerSupport,
                    coreContract.executors(),
                    this
                )
                _notifyInboxInitialized()
            } else {
                coreContract.logger().info("CRITICAL : No device ID found!")
            }
        }
    }

    /**
     * Phase 2: Reset method moved from CoreState
     * Resets the inbox and reinitializes it
     */
    fun reset() {
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            ctInboxController = null
        }
        // Schedule initialization on async executor
        coreContract.executors().postAsyncSafelyTask<Unit>().execute("initializeInbox") {
            initialize()
        }
    }
}

internal interface InboxLiveCallbacks {
    fun _notifyInboxMessagesDidUpdate()
    fun _notifyInboxInitialized()
}
