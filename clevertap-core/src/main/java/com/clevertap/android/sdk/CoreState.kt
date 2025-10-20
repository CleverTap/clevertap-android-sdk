package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.product_config.CTProductConfigFactory
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.video.VideoLibChecker

@Suppress("DEPRECATION")
internal open class CoreState(
    val context: Context,
    val locationManager: BaseLocationManager,
    val config: CleverTapInstanceConfig,
    val coreMetaData: CoreMetaData,
    val databaseManager: BaseDatabaseManager,
    val deviceInfo: DeviceInfo,
    val eventMediator: EventMediator,
    val localDataStore: LocalDataStore,
    val activityLifeCycleManager: ActivityLifeCycleManager,
    val analyticsManager: AnalyticsManager,
    val baseEventQueueManager: BaseEventQueueManager,
    val cTLockManager: CTLockManager,
    val callbackManager: BaseCallbackManager,
    val controllerManager: ControllerManager,
    val inAppController: InAppController,
    val evaluationManager: EvaluationManager,
    val impressionManager: ImpressionManager,
    val sessionManager: SessionManager,
    val validationResultStack: ValidationResultStack,
    val mainLooperHandler: MainLooperHandler,
    val networkManager: NetworkManager,
    val pushProviders: PushProviders,
    val varCache: VarCache,
    val parser: Parser,
    val cryptHandler: ICryptHandler,
    val storeRegistry: StoreRegistry,
    val templatesManager: TemplatesManager,
    val profileValueHandler: ProfileValueHandler,
    val cTVariables: CTVariables,
    val executors: CTExecutors,
    val contentFetchManager: ContentFetchManager,
    val loginInfoProvider: LoginInfoProvider
) {
    /**
     *
     *
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     *
     */
    @Deprecated("")
    fun getCtProductConfigController(context: Context?): CTProductConfigController? {
        if (this.config.isAnalyticsOnly) {
            this.config.getLogger()
                .debug(
                    this.config.accountId,
                    "Product Config is not enabled for this instance"
                )
            return null
        }
        if (this.controllerManager.ctProductConfigController == null) {
            this.config.getLogger().verbose(
                config.accountId + ":async_deviceID",
                "Initializing Product Config with device Id = " + this.deviceInfo.getDeviceID()
            )
            val ctProductConfigController = CTProductConfigFactory
                .getInstance(
                    context, this.deviceInfo,
                    this.config, analyticsManager, coreMetaData, callbackManager
                )
            this.controllerManager.ctProductConfigController = ctProductConfigController
        }
        return this.controllerManager.ctProductConfigController
    }

    /**
     * This method is responsible for switching user identity for clevertap.
     */
    fun asyncProfileSwitchUser(
        profile: Map<String, Any?>?,
        cacheGuid: String?,
        cleverTapID: String?
    ) {
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("resetProfile") {
            try {
                config.getLogger().verbose(
                    config.accountId,
                    "asyncProfileSwitchUser:[profile with Cached GUID $cacheGuid and cleverTapID $cleverTapID"
                )
                //set optOut to false on the current user to unregister the device token
                coreMetaData.isCurrentUserOptedOut = false
                // unregister the device token on the current user
                pushProviders.forcePushDeviceToken(false)

                // try and flush and then reset the queues
                baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR, null, true)
                baseEventQueueManager.flushQueueSync(
                    context,
                    EventGroup.PUSH_NOTIFICATION_VIEWED,
                    null,
                    true
                )
                contentFetchManager.cancelAllResponseJobs()
                databaseManager.clearQueues(context)

                // clear out the old data
                CoreMetaData.setActivityCount(1)
                sessionManager.destroySession()

                // either force restore the cached GUID or generate a new one
                if (cacheGuid != null) {
                    deviceInfo.forceUpdateDeviceId(cacheGuid)
                    callbackManager.notifyUserProfileInitialized(cacheGuid)
                } else if (config.enableCustomCleverTapId) {
                    deviceInfo.forceUpdateCustomCleverTapID(cleverTapID)
                } else {
                    deviceInfo.forceNewDeviceID()
                }

                localDataStore.changeUser()
                callbackManager.notifyUserProfileInitialized(deviceInfo.getDeviceID())

                // Restore state of opt out and system events from storage
                deviceInfo.setCurrentUserOptOutStateFromStorage()
                deviceInfo.setSystemEventsAllowedStateFromStorage()

                // variables for new user are fetched with App Launched
                resetVariables()
                analyticsManager.forcePushAppLaunchedEvent()
                if (profile != null) {
                    analyticsManager.pushProfile(profile)
                }
                pushProviders.forcePushDeviceToken(true)
                resetInbox()
                resetFeatureFlags()
                resetProductConfigs()
                recordDeviceIDErrors()
                resetDisplayUnits()

                notifyChangeUserCallback()

                controllerManager.inAppFCManager.changeUser(deviceInfo.getDeviceID())
            } catch (t: Throwable) {
                config.getLogger().verbose(config.accountId, "Reset Profile error", t)
            }
        }
    }

    fun notifyChangeUserCallback() {
        val changeUserCallbackList = callbackManager.getChangeUserCallbackList()
        synchronized(changeUserCallbackList) {
            for (callback in changeUserCallbackList) {
                callback?.onChangeUser(deviceInfo.getDeviceID(), config.accountId)
            }
        }
    }

    @Suppress("unused")
    fun onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (config.enableCustomCleverTapId) {
            if (cleverTapID == null) {
                Logger.i(
                    "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml Please call onUserlogin() and pass a custom CleverTap ID"
                )
            }
        } else {
            if (cleverTapID != null) {
                Logger.i(
                    "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml Please call CleverTapAPI.defaultInstance() without a custom CleverTap ID"
                )
            }
        }
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("_onUserLogin") {
            _onUserLogin(profile, cleverTapID)
        }
    }

    fun recordDeviceIDErrors() {
        for (validationResult in deviceInfo.getValidationResults()) {
            validationResultStack.pushValidationResult(validationResult)
        }
    }

    private fun _onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (profile == null) {
            return
        }

        try {
            val currentGUID = deviceInfo.getDeviceID()
            if (currentGUID == null) {
                return
            }

            var cachedGUID: String? = null
            var haveIdentifier = false

            // check for valid identifier keys
            // use the first one we find
            val iProfileHandler = IdentityRepoFactory
                .getRepo(context, config, validationResultStack)
            for (key in profile.keys) {
                val value = profile[key]
                val isProfileKey = iProfileHandler.hasIdentity(key)
                if (isProfileKey) {
                    try {
                        var identifier: String? = null
                        if (value != null) {
                            identifier = value.toString()
                        }
                        if (identifier != null && !identifier.isEmpty()) {
                            haveIdentifier = true
                            cachedGUID = loginInfoProvider.getGUIDForIdentifier(key, identifier)
                            if (cachedGUID != null) {
                                break
                            }
                        }
                    } catch (_: Throwable) {
                        // no-op
                    }
                }
            }

            // if no valid identifier provided or there are no identified users on the device; just push on the current profile
            if (!deviceInfo.isErrorDeviceId()) {
                if (!haveIdentifier || loginInfoProvider.isAnonymousDevice()) {
                    config.getLogger().debug(
                        config.accountId,
                        "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile"
                    )
                    analyticsManager.pushProfile(profile)
                    return
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID == currentGUID) {
                config.getLogger().debug(
                    config.accountId,
                    ("onUserLogin: " + profile + " maps to current device id " + currentGUID
                            + " pushing on current profile")
                )
                analyticsManager.pushProfile(profile)
                return
            }

            config.getLogger()
                .verbose(
                    config.accountId, ("onUserLogin: queuing reset profile for " + profile
                            + " with Cached GUID " + (cachedGUID ?: "NULL"))
                )

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID)
        } catch (t: Throwable) {
            config.getLogger().verbose(config.accountId, "onUserLogin failed", t)
        }
    }

    /**
     * Resets the Display Units in the cache
     */
    private fun resetDisplayUnits() {
        if (controllerManager.ctDisplayUnitController != null) {
            controllerManager.ctDisplayUnitController.reset()
        } else {
            config.getLogger().verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null"
            )
        }
    }

    private fun resetFeatureFlags() {
        val ctFeatureFlagsController = controllerManager.ctFeatureFlagsController
        if (ctFeatureFlagsController != null && ctFeatureFlagsController.isInitialized()) {
            ctFeatureFlagsController.resetWithGuid(deviceInfo.getDeviceID())
            ctFeatureFlagsController.fetchFeatureFlags()
        } else {
            config.getLogger().verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, CTFeatureFlagsController is null"
            )
        }
    }

    // always call async
    private fun resetInbox() {
        synchronized(cTLockManager.inboxControllerLock) {
            controllerManager.ctInboxController = null
        }
        initializeInbox()
    }

    @AnyThread
    fun initializeInbox() {
        if (config.isAnalyticsOnly) {
            config.getLogger()
                .debug(
                    config.accountId,
                    "Instance is analytics only, not initializing Notification Inbox"
                )
            return
        }
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("initializeInbox") { initializeInboxMain() }
    }

    // always call async
    @WorkerThread
    private fun initializeInboxMain() {
        synchronized(cTLockManager.inboxControllerLock) {
            if (controllerManager.ctInboxController != null) {
                callbackManager._notifyInboxInitialized()
                return
            }
            if (deviceInfo.getDeviceID() != null) {
                controllerManager.ctInboxController = CTInboxController(
                    config,
                    deviceInfo.getDeviceID(),
                    databaseManager.loadDBAdapter(context),
                    cTLockManager,
                    callbackManager,
                    VideoLibChecker.haveVideoPlayerSupport
                )
                callbackManager._notifyInboxInitialized()
            } else {
                config.getLogger().info("CRITICAL : No device ID found!")
            }
        }
    }


    //Session
    private fun resetProductConfigs() {
        if (config.isAnalyticsOnly) {
            config.getLogger()
                .debug(config.accountId, "Product Config is not enabled for this instance")
            return
        }
        if (controllerManager.ctProductConfigController != null) {
            controllerManager.ctProductConfigController.resetSettings()
        }
        val ctProductConfigController =
            CTProductConfigFactory.getInstance(
                context, deviceInfo, config, analyticsManager, coreMetaData,
                callbackManager
            )
        controllerManager.ctProductConfigController = ctProductConfigController
        config.getLogger().verbose(config.accountId, "Product Config reset")
    }

    private fun resetVariables() {
        if (controllerManager.ctVariables != null) {
            controllerManager.ctVariables.clearUserContent()
        }
    }
}