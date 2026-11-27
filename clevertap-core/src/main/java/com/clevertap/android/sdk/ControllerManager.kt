package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.displayunits.DisplayUnitCache
import com.clevertap.android.sdk.displayunits.NativeDisplayController
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.inbox.InboxDeleteCoordinator
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.video.VideoLibChecker
import org.json.JSONArray
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ControllerManager(
    private val context: Context,
    val config: CleverTapInstanceConfig,
    private val ctLockManager: CTLockManager,
    private val callbackManager: BaseCallbackManager,
    private val deviceInfo: DeviceInfo,
    private val baseDatabaseManager: BaseDatabaseManager
) {

    var inAppFCManager: InAppFCManager? = null

    var ndFCManager: NdFCManager? = null

    /**
     * The active [DisplayUnitCache]; `null` until the SDK receives its first `adUnit_notifs`
     * response or a host installs a cache via the setter.
     *
     * Replacing it: pass `null` to clear the reference (subsequent server responses will lazily
     * install a fresh default [CTDisplayUnitController]).
     */
    @Volatile
    var displayUnitCache: DisplayUnitCache? = null
        @Synchronized set

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    var ctFeatureFlagsController: CTFeatureFlagsController? = null

    var ctInboxController: CTInboxController? = null

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    var ctProductConfigController: CTProductConfigController? = null

    var inAppController: InAppController? = null

    var nativeDisplayController: NativeDisplayController? = null

    var pushProviders: PushProviders? = null

    var ctVariables: CTVariables? = null

    var inboxDeleteCoordinator: InboxDeleteCoordinator? = null

    /**
     * Returns the existing cache, or lazily installs a default
     * [CTDisplayUnitController] if none is present. The check-and-create
     * is atomic — prevents a race between the server-response thread and host
     * code calling the [displayUnitCache] setter.
     */
    @Synchronized
    fun getOrCreateDisplayUnitCache(): DisplayUnitCache =
        displayUnitCache ?: CTDisplayUnitController().also { displayUnitCache = it }

    /**
     * Returns the active cache only when it is the default [CTDisplayUnitController];
     * returns `null` when a host has installed a custom [DisplayUnitCache].
     */
    @Deprecated("since 8.3.0. Use displayUnitCache instead.")
    var ctDisplayUnitController: CTDisplayUnitController?
        get() = displayUnitCache as? CTDisplayUnitController
        set(ctDisplayUnitController) {
            displayUnitCache = ctDisplayUnitController
        }

    @AnyThread
    fun initializeInbox() {
        if (config.isAnalyticsOnly) {
            config.logger
                .debug(config.accountId, "Instance is analytics only, not initializing Notification Inbox")
            return
        }
        CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>().execute("initializeInbox") {
            initializeInboxLocked()
            null
        }
    }

    @WorkerThread
    fun initializeInboxSync() {
        if (config.isAnalyticsOnly) {
            config.logger
                .debug(config.accountId, "Instance is analytics only, not initializing Notification Inbox")
            return
        }
        initializeInboxLocked()
    }

    // always call async
    @WorkerThread
    private fun initializeInboxLocked() {
        synchronized(ctLockManager.inboxControllerLock) {
            if (ctInboxController != null) {
                callbackManager.notifyInboxInitialized()
                return
            }
            // Read the id once: Java re-read the volatile three times while building the controller.
            val deviceId = deviceInfo.deviceID
            if (deviceId == null) {
                config.logger.info("CRITICAL : No device ID found!")
                return
            }
            ctInboxController = CTInboxController(
                config,
                deviceId,
                baseDatabaseManager.loadDBAdapter(context),
                ctLockManager,
                callbackManager,
                VideoLibChecker.haveVideoPlayerSupport,
                inboxDeleteCoordinator
            )
            callbackManager.notifyInboxInitialized()
            inboxDeleteCoordinator?.retryPending(deviceId)
        }
    }

    fun invokeCallbacksForNetworkError() {
        // Variables
        ctVariables?.let { variables ->
            val fetchCallback = callbackManager.fetchVariablesCallback
            callbackManager.fetchVariablesCallback = null

            variables.handleVariableResponseError(fetchCallback)
        }

        // Add more callbacks if necessary
    }

    /**
     * Invokes the batch listener callback to notify about the completion of a batch operation.
     *
     * @param requestQueue The JSON array representing the batch request sent to server.
     * @param success      true when the batch operation was successful, false when no network or CleverTap
     * instance is set to offline mode or request is failed.
     */
    fun invokeBatchListener(requestQueue: JSONArray, success: Boolean) {
        callbackManager.batchListener?.onBatchSent(requestQueue, success)
    }
}
