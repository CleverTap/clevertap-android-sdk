package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CTInboxListener
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.response.InboxResponse
import org.json.JSONObject

/**
 * This class encapsulates the logic for handling the App Inbox feature of the CleverTap SDK.
 * It is responsible for processing inbox-related data from API responses, managing the lifecycle
 * of inbox messages, and notifying listeners about updates.
 *
 * The class interacts with various components:
 * - [CTLockManager] to ensure thread-safe operations.
 * - [InboxResponse] to parse and process inbox messages from server responses.
 * - [CTInboxController] to manage the state and data of the inbox.
 * - [CTInboxListener] to provide callbacks to the user about inbox initialization and updates.
 * - [CoreContract] to access core SDK functionalities like configuration and logging.
 *
 * It implements [CleverTapFeature] to integrate into the SDK's feature handling mechanism and
 * [InboxLiveCallbacks] to provide internal notifications about inbox state changes.
 *
 * @property cTLockManager Manages synchronization for thread safety.
 * @property inboxResponse Handles the parsing of inbox-related server responses.
 * @property mainPost A higher-order function to execute a given function on the main UI thread.
 *                    Defaults to using [Utils.runOnUiThread].
 */
internal data class InboxFeature(
    val cTLockManager: CTLockManager,
    val inboxResponse: InboxResponse,
    val mainPost: (() -> Unit) -> Unit = { func -> Utils.runOnUiThread { func } }
) : CleverTapFeature, InboxLiveCallbacks {
    var ctInboxController: CTInboxController? = null
    var inboxListener: CTInboxListener? = null

    lateinit var coreContract: CoreContract

    override fun _notifyInboxMessagesDidUpdate() {
        if (inboxListener != null) {
            mainPost(inboxListener!!::inboxMessagesDidUpdate)
        }
    }

    override fun _notifyInboxInitialized() {
        inboxListener?.inboxDidInitialize()
    }

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
    ) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing inbox messages"
            )
            return
        }
        inboxResponse.processResponse(response, stringBody, context)
    }
}

internal interface InboxLiveCallbacks {
    fun _notifyInboxMessagesDidUpdate()
    fun _notifyInboxInitialized()
}
