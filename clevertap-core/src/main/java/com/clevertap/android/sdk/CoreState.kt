package com.clevertap.android.sdk

import android.content.Context
import android.text.TextUtils
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.StorageHelper.putString
import com.clevertap.android.sdk.StoreProvider.Companion.getInstance
import com.clevertap.android.sdk.cryption.CryptMigrator
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.DataMigrationRepository
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsFactory
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
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
import com.clevertap.android.sdk.task.Task
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.Clock.Companion.SYSTEM
import com.clevertap.android.sdk.validation.ManifestValidator
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.variables.repo.VariablesRepo
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
    val loginInfoProvider: LoginInfoProvider,
    val storeProvider: StoreProvider,
    val variablesRepository: VariablesRepo,
    val clock: Clock = SYSTEM
) {

    internal var inAppFCManager: InAppFCManager? = null
    internal var ctInboxController: CTInboxController? = null

    fun setInAppFCManager(inAppFCManager: InAppFCManager) {
        this.inAppFCManager = inAppFCManager
        this.inAppController.setInAppFCManager(inAppFCManager)
        this.networkManager.setInAppFCManager(inAppFCManager)
    }

    fun getInAppFCManager() : InAppFCManager? {
        return inAppFCManager
    }

    fun getCTInboxController() : CTInboxController? {
        return ctInboxController
    }

    fun asyncStartup() {
        val fileResourceProviderInit = executors.ioTask<Unit>()
        fileResourceProviderInit.execute("initFileResourceProvider") {
            FileResourceProvider.getInstance(context, config.logger)
        }
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("migratingEncryption") {
            val dbAdapter = databaseManager.loadDBAdapter(context)
            val dataMigrationRepository = DataMigrationRepository(
                context = context,
                config = config,
                dbAdapter = dbAdapter
            )
            val cryptMigrator = CryptMigrator(
                logPrefix = config.accountId,
                configEncryptionLevel = config.encryptionLevel,
                logger = config.logger,
                cryptHandler = cryptHandler,
                cryptRepository = CryptRepository(
                    context = context,
                    accountId = config.accountId
                ),
                dataMigrationRepository = dataMigrationRepository,
                variablesRepo = variablesRepository,
                dbAdapter = dbAdapter
            )
            cryptMigrator.migrateEncryption()
        }
        deviceInfo.onInitDeviceInfo()

        val taskDeviceCachedInfo = executors.ioTask<Unit>()
        taskDeviceCachedInfo.execute("getDeviceCachedInfo"
        ) { deviceInfo.getDeviceCachedInfo() }

        val task1 = executors.ioTask<String>()
        // callback on main thread
        task1.addOnSuccessListener { deviceId: String ->
            config.logger.verbose(
                config.accountId + ":async_deviceID",
                "DeviceID initialized successfully!" + Thread.currentThread()
            )
            // No need to put getDeviceID() on background thread because prefs already loaded
            deviceIDCreated(deviceId)
        }
        task1.execute("initDeviceID") { deviceInfo.initDeviceID() }

        val taskInitStores = executors.ioTask<Unit>()
        taskInitStores.execute("initStores") {
            if (deviceInfo.getDeviceID() != null) {
                if (storeRegistry.inAppStore == null) {
                    val inAppStore: InAppStore = storeProvider.provideInAppStore(
                        context = context,
                        cryptHandler = cryptHandler,
                        deviceId = deviceInfo.getDeviceID(),
                        accountId = config.accountId
                    )
                    storeRegistry.inAppStore = inAppStore
                    evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds()
                    callbackManager.addChangeUserCallback(inAppStore)
                }
                if (storeRegistry.impressionStore == null) {
                    val impStore: ImpressionStore = storeProvider.provideImpressionStore(
                        context = context,
                        deviceId = deviceInfo.getDeviceID(),
                        accountId = config.accountId
                    )
                    storeRegistry.impressionStore = impStore
                    callbackManager.addChangeUserCallback(impStore)
                }
            }
        }

        //Get device id should be async to avoid strict mode policy.
        val taskInitFCManager = executors.ioTask<Unit>()
        taskInitFCManager.execute("initFCManager") {
            val deviceId = deviceInfo.deviceID
            if (deviceId != null && inAppFCManager == null) {
                config.logger
                    .verbose(
                        config.accountId + ":async_deviceID",
                        "Initializing InAppFC with device Id = $deviceId"
                    )
                setInAppFCManager(InAppFCManager(
                    context,
                    config,
                    deviceId,
                    storeRegistry,
                    impressionManager,
                    executors,
                    SYSTEM
                ))
            }
        }

        val taskVariablesInit = executors.ioTask<Unit>()
        taskVariablesInit.execute("initCTVariables") {
            cTVariables.init()
        }

        val taskInitFeatureFlags = executors.ioTask<Unit>()
        taskInitFeatureFlags.execute("initFeatureFlags") {
            initFeatureFlags(
                context,
                config,
                deviceInfo,
                callbackManager,
                analyticsManager
            )
        }

        val pushTask = executors.pushProviderTask<Unit>()
        pushTask.execute("asyncFindAvailableCTPushProviders") {
            pushProviders.initPushAmp()
            pushProviders.init()
        }
        asyncStartupp()
    }

    // todo rename better, picked from CleverTapAPI constructor.
    private fun asyncStartupp() {
        var task: Task<Unit> = executors.postAsyncSafelyTask<Unit>()
        task.execute("CleverTapAPI#initializeDeviceInfo") {
            if (config.isDefaultInstance) {
                ManifestValidator.validate(context, deviceInfo, pushProviders)
            }
        }

        val now = clock.currentTimeSecondsInt()
        if (now - CoreMetaData.getInitialAppEnteredForegroundTime() > 5) {
            config.setCreatedPostAppLaunch()
        }

        task = executors.postAsyncSafelyTask<Unit>()
        task.execute("setStatesAsync") {
            sessionManager.setLastVisitTime()
            sessionManager.setUserLastVisitTs()
            deviceInfo.setDeviceNetworkInfoReportingFromStorage()
            deviceInfo.setCurrentUserOptOutStateFromStorage()
            deviceInfo.setSystemEventsAllowedStateFromStorage()
        }

        task = executors.postAsyncSafelyTask<Unit>()
        task.execute("saveConfigtoSharedPrefs") {
            val configJson: String? = config.toJSONString()
            if (configJson == null) {
                Logger.v("Unable to save config to SharedPrefs, config Json is null")
                return@execute
            }
            putString(context, config.accountId, "instance", configJson)
        }
        task = executors.postAsyncSafelyTask<Unit>()
        task.execute("recordDeviceIDErrors") {
            if (deviceInfo.getDeviceID() != null) {
                recordDeviceIDErrors()
            }
        }
    }

    fun deviceIDCreated(deviceId: String) {
        val accountId: String = config.accountId

        /*if (callbackManager == null) {
            config.logger.verbose(
                "$accountId:async_deviceID",
                "ControllerManager not set yet! Returning from deviceIDCreated()"
            )
            return
        }*/

        val storeProvider = getInstance()

        // Inflate the local profile here as deviceId is required
        localDataStore.inflateLocalProfileAsync(context)

        // must move initStores task to async executor due to addChangeUserCallback synchronization
        val task: Task<Unit> = executors.ioTask<Unit>()
        task.execute("initStores") {
            if (storeRegistry.inAppStore == null) {
                val inAppStore = storeProvider.provideInAppStore(
                    context = context,
                    cryptHandler = cryptHandler,
                    deviceId = deviceId,
                    accountId = accountId
                )
                storeRegistry.inAppStore = inAppStore
                evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds()
                callbackManager.addChangeUserCallback(inAppStore)
            }
            if (storeRegistry.impressionStore == null) {
                val impStore = storeProvider.provideImpressionStore(
                    context = context,
                    deviceId = deviceId,
                    accountId = accountId
                )
                storeRegistry.impressionStore = impStore
                callbackManager.addChangeUserCallback(impStore)
            }
        }


        /*
          Reinitialising InAppFCManager with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        if (getInAppFCManager() == null) {
            config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing InAppFC after Device ID Created = $deviceId"
            )
            setInAppFCManager(
                InAppFCManager(
                    context, config, deviceId,
                    storeRegistry, impressionManager,
                    executors, SYSTEM
                )
            )
        }

        //todo : replace with variables
        /*
          Reinitialising product config & Feature Flag controllers with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        val ctFeatureFlagsController: CTFeatureFlagsController? = callbackManager
            .getCTFeatureFlagsController()

        if (ctFeatureFlagsController != null && TextUtils.isEmpty(ctFeatureFlagsController.getGuid())) {
            config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing Feature Flags after Device ID Created = $deviceId"
            )
            ctFeatureFlagsController.setGuidAndInit(deviceId)
        }
        //todo: replace with variables
        val ctProductConfigController: CTProductConfigController? = callbackManager
            .getCTProductConfigController()

        if (ctProductConfigController != null && TextUtils
                .isEmpty(ctProductConfigController.settings.guid)
        ) {
            config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing Product Config after Device ID Created = $deviceId"
            )
            ctProductConfigController.setGuidAndInit(deviceId)
        }
        config.logger.verbose(
            "$accountId:async_deviceID",
            "Got device id from DeviceInfo, notifying user profile initialized to SyncListener"
        )
        callbackManager.notifyUserProfileInitialized(deviceId)
        callbackManager.notifyCleverTapIDChanged(deviceId)
    }

    private fun initFeatureFlags(
        context: Context?,
        config: CleverTapInstanceConfig,
        deviceInfo: DeviceInfo,
        callbackManager: BaseCallbackManager,
        analyticsManager: AnalyticsManager?
    ) {
        config.logger.verbose(
            config.accountId + ":async_deviceID",
            "Initializing Feature Flags with device Id = " + deviceInfo.deviceID
        )
        if (config.isAnalyticsOnly) {
            config.logger.debug(config.accountId, "Feature Flag is not enabled for this instance")
        } else {
            callbackManager.ctFeatureFlagsController = CTFeatureFlagsFactory.getInstance(
                context,
                deviceInfo.deviceID,
                config, callbackManager, analyticsManager
            )
            config.logger.verbose(config.accountId + ":async_deviceID", "Feature Flags initialized")
        }
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
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
        if (this.callbackManager.ctProductConfigController == null) {
            this.config.getLogger().verbose(
                config.accountId + ":async_deviceID",
                "Initializing Product Config with device Id = " + this.deviceInfo.getDeviceID()
            )
            val ctProductConfigController = CTProductConfigFactory
                .getInstance(
                    context, this.deviceInfo,
                    this.config, analyticsManager, coreMetaData, callbackManager
                )
            this.callbackManager.ctProductConfigController = ctProductConfigController
        }
        return this.callbackManager.ctProductConfigController
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

                inAppFCManager?.changeUser(deviceInfo.getDeviceID())
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
        if (callbackManager.ctDisplayUnitController != null) {
            callbackManager.ctDisplayUnitController.reset()
        } else {
            config.getLogger().verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null"
            )
        }
    }

    private fun resetFeatureFlags() {
        val ctFeatureFlagsController = callbackManager.ctFeatureFlagsController
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
            ctInboxController = null
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
            if (ctInboxController != null) {
                callbackManager._notifyInboxInitialized()
                return
            }
            val deviceId = deviceInfo.getDeviceID()
            if (deviceId != null) {
                ctInboxController = CTInboxController(
                    deviceId,
                    databaseManager.loadDBAdapter(context),
                    cTLockManager,
                    callbackManager,
                    VideoLibChecker.haveVideoPlayerSupport,
                    executors
                )
                callbackManager.ctInboxController = ctInboxController
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
        if (callbackManager.ctProductConfigController != null) {
            callbackManager.ctProductConfigController.resetSettings()
        }
        val ctProductConfigController =
            CTProductConfigFactory.getInstance(
                context, deviceInfo, config, analyticsManager, coreMetaData,
                callbackManager
            )
        callbackManager.ctProductConfigController = ctProductConfigController
        config.getLogger().verbose(config.accountId, "Product Config reset")
    }

    private fun resetVariables() {
        cTVariables.clearUserContent()
    }
}