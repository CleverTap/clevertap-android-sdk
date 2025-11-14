package com.clevertap.android.sdk.features

import android.app.Activity
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CTInboxListener
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.InboxMessageButtonListener
import com.clevertap.android.sdk.InboxMessageListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inbox.CTInboxActivity
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.response.InboxResponse
import com.clevertap.android.sdk.task.Task
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

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
    private val mainPost: (() -> Unit) -> Unit = { func -> Utils.runOnUiThread { func } },
    private val currentActivityProvider: () -> Activity? = { CoreMetaData.getCurrentActivity() }
) : CleverTapFeature, InboxLiveCallbacks {
    
    var ctInboxController: CTInboxController? = null
    var inboxListener: CTInboxListener? = null
    private var inboxMessageButtonListener = WeakReference<InboxMessageButtonListener?>(null)
    private var inboxMessageListener = WeakReference<InboxMessageListener?>(null)

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

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
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

                handleApiData(response = jsonResponse)

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

    // ========== PUBLIC API FACADE ==========
    // These methods provide direct delegation from CleverTapAPI to Inbox functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Initializes the inbox controller and sends a callback to the CTInboxListener
     */
    fun initializeInbox() {
        coreContract.executors().postAsyncSafelyTask<Unit>().execute("initializeInbox") {
            initialize()
        }
    }

    /**
     * Returns an ArrayList of all CTInboxMessage objects
     */
    fun getAllInboxMessages(): ArrayList<CTInboxMessage> {
        Logger.d("InboxFeature:getAllInboxMessages: called")
        val inboxMessageArrayList = ArrayList<CTInboxMessage>()
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                val messageDAOArrayList = ctInboxController!!.messages
                for (messageDAO in messageDAOArrayList) {
                    Logger.v("CTMessage Dao - ${messageDAO.toJSON()}")
                    inboxMessageArrayList.add(CTInboxMessage(messageDAO.toJSON()))
                }
                return inboxMessageArrayList
            } else {
                logger.debug(accountId, "Notification Inbox not initialized")
                return inboxMessageArrayList // return empty list to avoid null pointer exceptions
            }
        }
    }

    /**
     * Returns an ArrayList of unread CTInboxMessage objects
     */
    fun getUnreadInboxMessages(): ArrayList<CTInboxMessage> {
        val inboxMessageArrayList = ArrayList<CTInboxMessage>()
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                val messageDAOArrayList = ctInboxController!!.unreadMessages
                for (messageDAO in messageDAOArrayList) {
                    inboxMessageArrayList.add(CTInboxMessage(messageDAO.toJSON()))
                }
                return inboxMessageArrayList
            } else {
                logger.debug(accountId, "Notification Inbox not initialized")
                return inboxMessageArrayList // return empty list to avoid null pointer exceptions
            }
        }
    }

    /**
     * Returns the CTInboxMessage object that belongs to the given message id
     */
    fun getInboxMessageForId(messageId: String?): CTInboxMessage? {
        Logger.d("InboxFeature:getInboxMessageForId() called with: messageId = [$messageId]")
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                val message = ctInboxController!!.getMessageForId(messageId)
                return if (message != null) CTInboxMessage(message.toJSON()) else null
            } else {
                logger.debug(accountId, "Notification Inbox not initialized")
                return null
            }
        }
    }

    /**
     * Returns the count of all inbox messages for the user
     */
    fun getInboxMessageCount(): Int {
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                return ctInboxController!!.count()
            } else {
                logger.debug(accountId, "Notification Inbox not initialized")
                return -1
            }
        }
    }

    /**
     * Returns the count of total number of unread inbox messages for the user
     */
    fun getInboxMessageUnreadCount(): Int {
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController != null) {
                return ctInboxController!!.unreadCount()
            } else {
                logger.debug(accountId, "Notification Inbox not initialized")
                return -1
            }
        }
    }

    /**
     * Deletes the given CTInboxMessage object
     */
    fun deleteInboxMessage(message: CTInboxMessage?) {
        if (ctInboxController != null) {
            ctInboxController!!.deleteInboxMessage(message)
        } else {
            logger.debug(accountId, "Notification Inbox not initialized")
        }
    }

    /**
     * Deletes the CTInboxMessage object for given messageId
     */
    fun deleteInboxMessage(messageId: String?) {
        val message = getInboxMessageForId(messageId)
        deleteInboxMessage(message)
    }

    /**
     * Deletes multiple CTInboxMessage objects for given list of messageIDs
     */
    fun deleteInboxMessagesForIDs(messageIDs: ArrayList<String>?) {
        if (ctInboxController != null) {
            ctInboxController!!.deleteInboxMessagesForIDs(messageIDs)
        } else {
            logger.debug(accountId, "Notification Inbox not initialized")
        }
    }

    /**
     * Marks the given CTInboxMessage object as read
     */
    fun markReadInboxMessage(message: CTInboxMessage?) {
        if (ctInboxController != null) {
            ctInboxController!!.markReadInboxMessage(message)
        } else {
            logger.debug(accountId, "Notification Inbox not initialized")
        }
    }

    /**
     * Marks the given messageId of CTInboxMessage object as read
     */
    fun markReadInboxMessage(messageId: String?) {
        val message = getInboxMessageForId(messageId)
        markReadInboxMessage(message)
    }

    /**
     * Marks multiple CTInboxMessage objects as read for given list of messageIDs
     */
    fun markReadInboxMessagesForIDs(messageIDs: ArrayList<String>?) {
        if (ctInboxController != null) {
            ctInboxController!!.markReadInboxMessagesForIDs(messageIDs)
        } else {
            logger.debug(accountId, "Notification Inbox not initialized")
        }
    }

    /**
     * Opens CTInboxActivity to display Inbox Messages without a custom style
     */
    fun showAppInbox() {
        showAppInbox(CTInboxStyleConfig())
    }

    /**
     * Opens CTInboxActivity to display Inbox Messages
     */
    fun showAppInbox(styleConfig: CTInboxStyleConfig) {
        synchronized(coreContract.ctLockManager().inboxControllerLock) {
            if (ctInboxController == null) {
                logger.debug(accountId, "Notification Inbox not initialized")
                return
            }
        }

        // make styleConfig immutable
        val _styleConfig = CTInboxStyleConfig(styleConfig)

        val intent = android.content.Intent(coreContract.context(), CTInboxActivity::class.java)
        intent.putExtra("styleConfig", _styleConfig)
        val configBundle = Bundle()
        configBundle.putParcelable("config", coreContract.config())
        intent.putExtra("configBundle", configBundle)
        try {
            val currentActivity = currentActivityProvider()
            if (currentActivity == null) {
                throw IllegalStateException("Current activity reference not found")
            }
            currentActivity.startActivity(intent)
            Logger.d("Displaying Notification Inbox")

        } catch (t: Throwable) {
            Logger.v(
                "Please verify the integration of your app." +
                        " It is not setup to support Notification Inbox yet.", t
            )
        }
    }

    /**
     * Dismisses the App Inbox Activity if already opened
     */
    fun dismissAppInbox() {
        try {
            val appInboxActivity = coreContract.coreMetaData().appInboxActivity
            if (appInboxActivity == null) {
                throw IllegalStateException("AppInboxActivity reference not found")
            }
            if (!appInboxActivity.isFinishing) {
                logger.verbose(accountId, "Finishing the App Inbox")
                appInboxActivity.finish()
            }
        } catch (t: Throwable) {
            logger.verbose(
                accountId, "Can't dismiss AppInbox, please ensure to call this method after the usage of " +
                        "cleverTapApiInstance.showAppInbox(). \n$t"
            )
        }
    }

    fun messageDidShow(
        inboxMessage: CTInboxMessage,
        data: Bundle?
    ) {
        val task: Task<Unit> = coreContract.executors().postAsyncSafelyTask<Unit>()
        task.execute("handleMessageDidShow") {
            Logger.d("CleverTapAPI:messageDidShow() called  in async with: messageId = [" + inboxMessage.messageId + "]")
            val message = getInboxMessageForId(inboxMessage.messageId)
            if (!message!!.isRead) {
                markReadInboxMessage(inboxMessage)
                coreContract.analytics().pushInboxMessageStateEvent(false, inboxMessage, data)
            }
        }
    }

    fun messageDidClick(
        contentPageIndex: Int,
        inboxMessage: CTInboxMessage,
        data: Bundle?,
        keyValue: HashMap<String?, String?>?,
        buttonIndex: Int
    ) {
        coreContract.analytics().pushInboxMessageStateEvent(true, inboxMessage, data)

        Logger.v("clicked inbox notification.")

        // notify the onInboxItemClicked callback if the listener is set/non-null.
        inboxMessageListener.get()?.onInboxItemClicked(inboxMessage, contentPageIndex, buttonIndex)

        if (keyValue != null && !keyValue.isEmpty()) {
            Logger.v("clicked button of an inbox notification.")
            // notify the onInboxButtonClick callback if the listener is set/non-null.
            inboxMessageButtonListener.get()?.onInboxButtonClick(keyValue)
        }
    }

    fun setInboxMessageButtonListener(listener: InboxMessageButtonListener?) {
        this.inboxMessageButtonListener = WeakReference<InboxMessageButtonListener?>(listener)
    }

    fun setCTInboxMessageListener(listener: InboxMessageListener?) {
        this.inboxMessageListener = WeakReference<InboxMessageListener?>(listener)
    }

    // ========== PUBLIC API FACADE END ==========
}

internal interface InboxLiveCallbacks {
    fun _notifyInboxMessagesDidUpdate()
    fun _notifyInboxInitialized()
}
