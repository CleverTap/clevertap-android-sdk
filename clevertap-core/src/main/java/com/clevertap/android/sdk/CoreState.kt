package com.clevertap.android.sdk

import android.content.Context
import android.text.TextUtils
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.StorageHelper.putString
import com.clevertap.android.sdk.cryption.CryptMigrator
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.DataMigrationRepository
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsFactory
import com.clevertap.android.sdk.features.AnalyticsFeature
import com.clevertap.android.sdk.features.CallbackFeature
import com.clevertap.android.sdk.features.CoreFeature
import com.clevertap.android.sdk.features.DataFeature
import com.clevertap.android.sdk.features.DisplayUnitFeature
import com.clevertap.android.sdk.features.FeatureFlagFeature
import com.clevertap.android.sdk.features.InAppFeature
import com.clevertap.android.sdk.features.InboxFeature
import com.clevertap.android.sdk.features.LifecycleFeature
import com.clevertap.android.sdk.features.NetworkFeature
import com.clevertap.android.sdk.features.ProductConfigFeature
import com.clevertap.android.sdk.features.ProfileFeature
import com.clevertap.android.sdk.features.PushFeature
import com.clevertap.android.sdk.features.VariablesFeature
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
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.product_config.CTProductConfigFactory
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.task.Task
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ManifestValidator
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import com.clevertap.android.sdk.video.VideoLibChecker
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Suppress("DEPRECATION")
internal open class CoreState(
    val core: CoreFeature,
    val data: DataFeature,
    val network: NetworkFeature,
    val analytics: AnalyticsFeature,
    val profileFeat: ProfileFeature,
    val inApp: InAppFeature,
    val inbox: InboxFeature,
    val variables: VariablesFeature,
    val push: PushFeature,
    val lifecycle: LifecycleFeature,
    val callback: CallbackFeature,
    val productConfig: ProductConfigFeature,
    val displayUnitF: DisplayUnitFeature,
    val featureFlagF: FeatureFlagFeature
) : CoreContract {

    init {
        network.coreContract(this)
    }

    // Backward compatibility accessors - delegate to feature groups
    val context: Context get() = core.context
    val locationManager: BaseLocationManager get() = profileFeat.locationManager
    val config: CleverTapInstanceConfig get() = core.config
    val coreMetaData: CoreMetaData get() = core.coreMetaData
    val databaseManager: BaseDatabaseManager get() = data.databaseManager
    val deviceInfo: DeviceInfo get() = core.deviceInfo
    val eventMediator: EventMediator get() = analytics.eventMediator
    val localDataStore: LocalDataStore get() = data.localDataStore
    val activityLifeCycleManager: ActivityLifeCycleManager get() = lifecycle.activityLifeCycleManager
    val analyticsManager: AnalyticsManager get() = analytics.analyticsManager
    val baseEventQueueManager: BaseEventQueueManager get() = analytics.baseEventQueueManager
    val cTLockManager: CTLockManager get() = inbox.cTLockManager
    val callbackManager: BaseCallbackManager get() = callback.callbackManager
    val inAppController: InAppController get() = inApp.inAppController
    val evaluationManager: EvaluationManager get() = inApp.evaluationManager
    val impressionManager: ImpressionManager get() = inApp.impressionManager
    val sessionManager: SessionManager get() = analytics.sessionManager
    val validationResultStack: ValidationResultStack get() = core.validationResultStack
    val mainLooperHandler: MainLooperHandler get() = core.mainLooperHandler
    val networkManager: NetworkManager get() = network.networkManager
    val pushProviders: PushProviders get() = push.pushProviders
    val varCache: VarCache get() = variables.varCache
    val parser: Parser get() = variables.parser
    val cryptHandler: ICryptHandler get() = core.cryptHandler
    val storeRegistry: StoreRegistry get() = data.storeRegistry
    val templatesManager: TemplatesManager get() = inApp.templatesManager
    val profileValueHandler: ProfileValueHandler get() = profileFeat.profileValueHandler
    val cTVariables: CTVariables get() = variables.cTVariables
    val executors: CTExecutors get() = core.executors
    val contentFetchManager: ContentFetchManager get() = network.contentFetchManager
    val loginInfoProvider: LoginInfoProvider get() = profileFeat.loginInfoProvider
    val storeProvider: StoreProvider get() = data.storeProvider
    val variablesRepository: VariablesRepo get() = variables.variablesRepository
    val clock: Clock get() = core.clock
    
    internal var inAppFCManager: InAppFCManager?
        get() = inApp.inAppFCManager
        set(value) { inApp.inAppFCManager = value }
    
    internal var ctInboxController: CTInboxController?
        get() = inbox.ctInboxController
        set(value) { inbox.ctInboxController = value }

    fun initInAppFCManager(deviceId: String) {
        val iam = InAppFCManager(
            core.context, core.config, deviceId,
            data.storeRegistry, inApp.impressionManager,
            core.executors, core.clock
        )
        core.executors.postAsyncSafelyTask<Unit>().execute("initInAppFCManager") {
            iam.init(deviceId)
        }
        this.inApp.inAppFCManager = iam
        this.inApp.inAppController.setInAppFCManager(iam)
        this.network.networkManager.setInAppFCManager(iam)
    }

    fun getInAppFCManager() : InAppFCManager? {
        return inApp.inAppFCManager
    }

    fun getCTInboxController() : CTInboxController? {
        return inbox.ctInboxController
    }

    fun asyncStartup() {
        val fileResourceProviderInit = core.executors.ioTask<Unit>()
        fileResourceProviderInit.execute("initFileResourceProvider") {
            FileResourceProvider.getInstance(core.context, core.config.logger)
        }
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("migratingEncryption") {
            val dbAdapter = data.databaseManager.loadDBAdapter(core.context)
            val dataMigrationRepository = DataMigrationRepository(
                context = core.context,
                config = core.config,
                dbAdapter = dbAdapter
            )
            val cryptMigrator = CryptMigrator(
                logPrefix = core.config.accountId,
                configEncryptionLevel = core.config.encryptionLevel,
                logger = core.config.logger,
                cryptHandler = core.cryptHandler,
                cryptRepository = CryptRepository(
                    context = core.context,
                    accountId = core.config.accountId
                ),
                dataMigrationRepository = dataMigrationRepository,
                variablesRepo = variables.variablesRepository,
                dbAdapter = dbAdapter
            )
            cryptMigrator.migrateEncryption()
        }

        core.config.logger.verbose(config.accountId + ":async_deviceID", "DeviceInfo() called")
        val taskDeviceCachedInfo = core.executors.ioTask<Unit>()
        taskDeviceCachedInfo.execute("getDeviceCachedInfo"
        ) { core.deviceInfo.getDeviceCachedInfo() }

        val task1 = core.executors.ioTask<String>()
        // callback on main thread
        task1.addOnSuccessListener { deviceId: String ->
            core.config.logger.verbose(
                core.config.accountId + ":async_deviceID",
                "DeviceID initialized successfully!" + Thread.currentThread()
            )
            // No need to put getDeviceID() on background thread because prefs already loaded
            deviceIDCreated(deviceId)
        }
        task1.execute("initDeviceID") { core.deviceInfo.initDeviceID() }

        val taskInitStores = core.executors.ioTask<Unit>()
        taskInitStores.execute("initStores") {
            initInAppStores(core.deviceInfo.getDeviceID())
        }

        //Get device id should be async to avoid strict mode policy.
        val taskInitFCManager = core.executors.ioTask<Unit>()
        taskInitFCManager.execute("initFCManager") {
            val deviceId = core.deviceInfo.deviceID
            if (deviceId != null && inApp.inAppFCManager == null) {
                core.config.logger
                    .verbose(
                        core.config.accountId + ":async_deviceID",
                        "Initializing InAppFC with device Id = $deviceId"
                    )
                initInAppFCManager(deviceId)
            }
        }

        val taskVariablesInit = core.executors.ioTask<Unit>()
        taskVariablesInit.execute("initCTVariables") {
            variables.cTVariables.init()
        }

        val taskInitFeatureFlags = core.executors.ioTask<Unit>()
        taskInitFeatureFlags.execute("initFeatureFlags") {
            initFeatureFlags(
                core.context,
                core.config,
                core.deviceInfo,
                callback.callbackManager,
                analytics.analyticsManager
            )
        }

        val pushTask = core.executors.pushProviderTask<Unit>()
        pushTask.execute("asyncFindAvailableCTPushProviders") {
            push.pushProviders.initPushAmp()
            push.pushProviders.init()
        }
        core.executors.postAsyncSafelyTask<Unit>().execute("CleverTapAPI#initializeDeviceInfo") {
            if (core.config.isDefaultInstance) {
                ManifestValidator.validate(core.context, core.deviceInfo, push.pushProviders)
            }
        }

        val now = core.clock.currentTimeSecondsInt()
        if (now - CoreMetaData.getInitialAppEnteredForegroundTime() > 5) {
            core.config.setCreatedPostAppLaunch()
        }

        core.executors.postAsyncSafelyTask<Unit>().execute("setStatesAsync") {
            analytics.sessionManager.setLastVisitTime()
            analytics.sessionManager.setUserLastVisitTs()
            core.deviceInfo.setDeviceNetworkInfoReportingFromStorage()
            core.deviceInfo.setCurrentUserOptOutStateFromStorage()
            core.deviceInfo.setSystemEventsAllowedStateFromStorage()
        }

        core.executors.postAsyncSafelyTask<Unit>().execute("saveConfigtoSharedPrefs") {
            val configJson: String? = core.config.toJSONString()
            if (configJson == null) {
                Logger.v("Unable to save config to SharedPrefs, config Json is null")
                return@execute
            }
            putString(core.context, core.config.accountId, "instance", configJson)
        }
        core.executors.postAsyncSafelyTask<Unit>().execute("recordDeviceIDErrors") {
            if (core.deviceInfo.getDeviceID() != null) {
                recordDeviceIDErrors()
            }
        }
    }

    @WorkerThread
    private fun initInAppStores(deviceId: String?) {
        if (deviceId != null) {
            if (data.storeRegistry.inAppStore == null) {
                val inAppStore: InAppStore = data.storeProvider.provideInAppStore(
                    context = core.context,
                    cryptHandler = core.cryptHandler,
                    deviceId = deviceId,
                    accountId = core.config.accountId
                )
                data.storeRegistry.inAppStore = inAppStore
                inApp.evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds()
                callback.callbackManager.addChangeUserCallback(inAppStore)
            }
            if (data.storeRegistry.impressionStore == null) {
                val impStore: ImpressionStore = data.storeProvider.provideImpressionStore(
                    context = core.context,
                    deviceId = deviceId,
                    accountId = core.config.accountId
                )
                data.storeRegistry.impressionStore = impStore
                callback.callbackManager.addChangeUserCallback(impStore)
            }
        }
    }

    fun deviceIDCreated(deviceId: String) {
        val accountId: String = core.config.accountId

        // Inflate the local profile here as deviceId is required
        data.localDataStore.inflateLocalProfileAsync(core.context)

        // must move initStores task to async executor due to addChangeUserCallback synchronization
        val task: Task<Unit> = core.executors.ioTask<Unit>()
        task.execute("initStores") {
            initInAppStores(deviceId)
        }

        /*
          Reinitialising InAppFCManager with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        if (inApp.inAppFCManager == null) {
            core.config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing InAppFC after Device ID Created = $deviceId"
            )
            initInAppFCManager(deviceId)
        }

        //todo : replace with variables
        /*
          Reinitialising product config & Feature Flag controllers with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        val ctFeatureFlagsController = featureFlagF.ctFeatureFlagsController

        if (ctFeatureFlagsController != null && TextUtils.isEmpty(ctFeatureFlagsController.getGuid())) {
            core.config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing Feature Flags after Device ID Created = $deviceId"
            )
            ctFeatureFlagsController.setGuidAndInit(deviceId)
        }
        //todo: replace with variables
        val ctProductConfigController = productConfig.productConfigController

        if (ctProductConfigController != null && TextUtils
                .isEmpty(ctProductConfigController.settings.guid)
        ) {
            core.config.logger.verbose(
                "$accountId:async_deviceID",
                "Initializing Product Config after Device ID Created = $deviceId"
            )
            ctProductConfigController.setGuidAndInit(deviceId)
        }
        core.config.logger.verbose(
            "$accountId:async_deviceID",
            "Got device id from DeviceInfo, notifying user profile initialized to SyncListener"
        )
        callback.callbackManager.notifyUserProfileInitialized(deviceId)
        callback.callbackManager.notifyCleverTapIDChanged(deviceId)
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
            featureFlagF.ctFeatureFlagsController = CTFeatureFlagsFactory.getInstance(
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
        if (core.config.isAnalyticsOnly) {
            core.config.getLogger()
                .debug(
                    core.config.accountId,
                    "Product Config is not enabled for this instance"
                )
            return null
        }
        if (productConfig.productConfigController == null) {
            core.config.getLogger().verbose(
                core.config.accountId + ":async_deviceID",
                "Initializing Product Config with device Id = " + core.deviceInfo.getDeviceID()
            )
            val ctProductConfigController = CTProductConfigFactory
                .getInstance(
                    context, core.deviceInfo,
                    core.config, analytics.analyticsManager, core.coreMetaData, callback.callbackManager
                )
            productConfig.productConfigController = ctProductConfigController
        }
        return productConfig.productConfigController
    }

    /**
     * This method is responsible for switching user identity for clevertap.
     */
    fun asyncProfileSwitchUser(
        profile: Map<String, Any?>?,
        cacheGuid: String?,
        cleverTapID: String?
    ) {
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("resetProfile") {
            try {
                core.config.getLogger().verbose(
                    core.config.accountId,
                    "asyncProfileSwitchUser:[profile with Cached GUID $cacheGuid and cleverTapID $cleverTapID"
                )
                //set optOut to false on the current user to unregister the device token
                core.coreMetaData.isCurrentUserOptedOut = false
                // unregister the device token on the current user
                push.pushProviders.forcePushDeviceToken(false)

                // try and flush and then reset the queues
                analytics.baseEventQueueManager.flushQueueSync(core.context, EventGroup.REGULAR, null, true)
                analytics.baseEventQueueManager.flushQueueSync(
                    core.context,
                    EventGroup.PUSH_NOTIFICATION_VIEWED,
                    null,
                    true
                )
                network.contentFetchManager.cancelAllResponseJobs()
                data.databaseManager.clearQueues(core.context)

                // clear out the old data
                CoreMetaData.setActivityCount(1)
                analytics.sessionManager.destroySession()

                // either force restore the cached GUID or generate a new one
                if (cacheGuid != null) {
                    core.deviceInfo.forceUpdateDeviceId(cacheGuid)
                    callback.callbackManager.notifyUserProfileInitialized(cacheGuid)
                } else if (core.config.enableCustomCleverTapId) {
                    core.deviceInfo.forceUpdateCustomCleverTapID(cleverTapID)
                } else {
                    core.deviceInfo.forceNewDeviceID()
                }

                data.localDataStore.changeUser()
                callback.callbackManager.notifyUserProfileInitialized(core.deviceInfo.getDeviceID())

                // Restore state of opt out and system events from storage
                core.deviceInfo.setCurrentUserOptOutStateFromStorage()
                core.deviceInfo.setSystemEventsAllowedStateFromStorage()

                // variables for new user are fetched with App Launched
                resetVariables()
                analytics.analyticsManager.forcePushAppLaunchedEvent()
                if (profile != null) {
                    analytics.analyticsManager.pushProfile(profile)
                }
                push.pushProviders.forcePushDeviceToken(true)
                resetInbox()
                resetFeatureFlags()
                resetProductConfigs()
                recordDeviceIDErrors()
                resetDisplayUnits()

                notifyChangeUserCallback()

                inApp.inAppFCManager?.changeUser(core.deviceInfo.getDeviceID())
            } catch (t: Throwable) {
                core.config.getLogger().verbose(core.config.accountId, "Reset Profile error", t)
            }
        }
    }

    fun notifyChangeUserCallback() {
        val changeUserCallbackList = callback.callbackManager.getChangeUserCallbackList()
        synchronized(changeUserCallbackList) {
            for (callback in changeUserCallbackList) {
                callback?.onChangeUser(core.deviceInfo.getDeviceID(), core.config.accountId)
            }
        }
    }

    @Suppress("unused")
    fun onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (core.config.enableCustomCleverTapId) {
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
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("_onUserLogin") {
            _onUserLogin(profile, cleverTapID)
        }
    }

    fun recordDeviceIDErrors() {
        for (validationResult in core.deviceInfo.getValidationResults()) {
            core.validationResultStack.pushValidationResult(validationResult)
        }
    }

    private fun _onUserLogin(profile: Map<String, Any?>?, cleverTapID: String?) {
        if (profile == null) {
            return
        }

        try {
            val currentGUID = core.deviceInfo.getDeviceID()
            if (currentGUID == null) {
                return
            }

            var cachedGUID: String? = null
            var haveIdentifier = false

            // check for valid identifier keys
            // use the first one we find
            val iProfileHandler = IdentityRepoFactory
                .getRepo(core.context, core.config, core.validationResultStack)
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
                            cachedGUID = profileFeat.loginInfoProvider.getGUIDForIdentifier(key, identifier)
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
            if (!core.deviceInfo.isErrorDeviceId()) {
                if (!haveIdentifier || profileFeat.loginInfoProvider.isAnonymousDevice()) {
                    core.config.getLogger().debug(
                        core.config.accountId,
                        "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile"
                    )
                    analytics.analyticsManager.pushProfile(profile)
                    return
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID == currentGUID) {
                core.config.getLogger().debug(
                    core.config.accountId,
                    ("onUserLogin: " + profile + " maps to current device id " + currentGUID
                            + " pushing on current profile")
                )
                analytics.analyticsManager.pushProfile(profile)
                return
            }

            core.config.getLogger()
                .verbose(
                    core.config.accountId, ("onUserLogin: queuing reset profile for " + profile
                            + " with Cached GUID " + (cachedGUID ?: "NULL"))
                )

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID)
        } catch (t: Throwable) {
            core.config.getLogger().verbose(core.config.accountId, "onUserLogin failed", t)
        }
    }

    /**
     * Resets the Display Units in the cache
     */
    private fun resetDisplayUnits() {
        if (displayUnitF.controller != null) {
            displayUnitF.controller!!.reset()
        } else {
            core.config.getLogger().verbose(
                core.config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null"
            )
        }
    }

    private fun resetFeatureFlags() {
        val ctFeatureFlagsController = featureFlagF.ctFeatureFlagsController
        if (ctFeatureFlagsController != null && ctFeatureFlagsController.isInitialized()) {
            ctFeatureFlagsController.resetWithGuid(core.deviceInfo.getDeviceID())
            ctFeatureFlagsController.fetchFeatureFlags()
        } else {
            core.config.getLogger().verbose(
                core.config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, CTFeatureFlagsController is null"
            )
        }
    }

    // always call async
    private fun resetInbox() {
        synchronized(inbox.cTLockManager.inboxControllerLock) {
            inbox.ctInboxController = null
        }
        initializeInbox()
    }

    @AnyThread
    fun initializeInbox() {
        if (core.config.isAnalyticsOnly) {
            core.config.getLogger()
                .debug(
                    core.config.accountId,
                    "Instance is analytics only, not initializing Notification Inbox"
                )
            return
        }
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("initializeInbox") { initializeInboxMain() }
    }

    // always call async
    @WorkerThread
    private fun initializeInboxMain() {
        synchronized(inbox.cTLockManager.inboxControllerLock) {
            if (inbox.ctInboxController != null) {
                inbox._notifyInboxInitialized()
                return
            }
            val deviceId = core.deviceInfo.getDeviceID()
            if (deviceId != null) {
                inbox.ctInboxController = CTInboxController(
                    deviceId,
                    data.databaseManager.loadDBAdapter(core.context),
                    inbox.cTLockManager,
                    VideoLibChecker.haveVideoPlayerSupport,
                    core.executors,
                    inbox
                )
                inbox._notifyInboxInitialized()
            } else {
                core.config.getLogger().info("CRITICAL : No device ID found!")
            }
        }
    }

    //Session
    private fun resetProductConfigs() {
        if (core.config.isAnalyticsOnly) {
            core.config.getLogger()
                .debug(core.config.accountId, "Product Config is not enabled for this instance")
            return
        }
        productConfig.productConfigController?.resetSettings()
        val ctProductConfigController =
            CTProductConfigFactory.getInstance(
                core.context, core.deviceInfo, core.config, analytics.analyticsManager, core.coreMetaData,
                callback.callbackManager
            )
        productConfig.productConfigController = ctProductConfigController
        core.config.getLogger().verbose(core.config.accountId, "Product Config reset")
    }

    private fun resetVariables() {
        variables.cTVariables.clearUserContent()
    }

    override fun handleSendQueueResponse(
        response: Response,
        isFullResponse: Boolean,
        requestBody: SendQueueRequestBody,
        endpointId: EndpointId,
        isUserSwitchFlush: Boolean
    ) {
        var bodyString: String? = response.readBody()
        var bodyJson: JSONObject? = bodyString.toJsonOrNull()

        core.config.logger.verbose(config.accountId, "Processing response : $bodyJson")

        if (bodyString.isNullOrBlank() || bodyJson == null) {
            callback.callbackManager.invokeBatchListener(requestBody.queue, true)
            return
        }

        // Handle decryption if needed
        val isEncryptedResponse = response.getHeaderValue(
            com.clevertap.android.sdk.network.api.CtApi.HEADER_ENCRYPTION_ENABLED
        ).toBoolean()

        if (isEncryptedResponse) {
            when (val decryptResponse = network.encryptionManager.decryptResponse(bodyString = bodyString)) {
                is com.clevertap.android.sdk.network.api.EncryptionFailure -> {
                    core.config.logger.verbose(config.accountId, "Failed to decrypt response")
                    callback.callbackManager.invokeBatchListener(requestBody.queue, false)
                    return
                }
                is com.clevertap.android.sdk.network.api.EncryptionSuccess -> {
                    bodyString = decryptResponse.data
                    bodyJson = bodyString.toJsonOrNull()
                    core.config.logger.verbose("Decrypted response = $bodyString")
                }
            }
        }

        // Delegate to response handler
        network.clevertapResponseHandler.handleResponse(
            isFullResponse,
            bodyJson,
            bodyString,
            isUserSwitchFlush
        )

        // Notify success
        callback.callbackManager.invokeBatchListener(requestBody.queue, true)
    }

    override fun handleVariablesResponse(response: Response) {
        val bodyString = response.readBody()
        val bodyJson = bodyString.toJsonOrNull()

        core.config.logger.verbose(config.accountId, "Processing variables response : $bodyJson")

        // Process through ARP response handler
        network.arpResponse.processResponse(bodyJson, bodyString, core.context)
    }

    override fun handlePushImpressionsResponse(response: Response, queue: JSONArray) {
        core.config.logger.verbose(
            config.accountId,
            "Processing push impressions response : ${response.readBody().toJsonOrNull()}"
        )

        // Notify listeners for push impressions
        notifyListenersForPushImpressionSentToServer(queue)
    }

    override fun onNetworkError() {
        variables.invokeCallbacksForNetworkError()
    }

    override fun onNetworkSuccess(queue: JSONArray, success: Boolean) {
        callback.callbackManager.invokeBatchListener(queue, success)
    }

    override fun onFlushFailure(context: Context) {
        callback.callbackManager.failureFlushListener?.failureFlush(context)
    }

    override fun notifyHeadersSent(allHeaders: JSONObject, endpointId: EndpointId) {
        val networkHeadersListeners = network.networkHeadersListeners
        for (listener in networkHeadersListeners) {
            listener.onSentHeaders(allHeaders, endpointId)
        }
    }

    override fun getHeadersToAttach(endpointId: EndpointId): JSONObject? {
        val networkHeadersListeners = network.networkHeadersListeners
        val combinedHeaders = JSONObject()

        for (listener in networkHeadersListeners) {
            val headersToAttach = listener.onAttachHeaders(endpointId)
            if (headersToAttach != null) {
                combinedHeaders.copyFrom(headersToAttach)
            }
        }

        return if (combinedHeaders.length() > 0) combinedHeaders else null
    }

    override fun notifySCDomainAvailable(domain: String) {
        callback.callbackManager.scDomainListener?.onSCDomainAvailable(domain)
    }

    override fun notifySCDomainUnavailable() {
        callback.callbackManager.scDomainListener?.onSCDomainUnavailable()
    }

    override fun didNotFlush() {
        variables.invokeCallbacksForNetworkError()
        callbackManager.invokeBatchListener(JSONArray(), false)
    }

    // ============ CORE DEPENDENCIES ACCESS ============

    override fun context(): Context = core.context
    override fun config(): CleverTapInstanceConfig = core.config
    override fun deviceInfo(): DeviceInfo = core.deviceInfo
    override fun coreMetaData(): CoreMetaData = core.coreMetaData
    override fun database(): BaseDatabaseManager = data.databaseManager
    override fun logger(): ILogger = core.config.logger
    override fun analytics(): AnalyticsManager = analytics.analyticsManager

    // ============ HELPER METHODS ============

    @Throws(JSONException::class)
    private fun notifyListenersForPushImpressionSentToServer(queue: JSONArray) {
        for (i in 0..<queue.length()) {
            try {
                val notif = queue.getJSONObject(i).optJSONObject("evtData")
                if (notif != null) {
                    val pushId = notif.optString(Constants.WZRK_PUSH_ID)
                    val pushAccountId = notif.optString(Constants.WZRK_ACCT_ID_KEY)

                    notifyListenerForPushImpressionSentToServer(
                        com.clevertap.android.sdk.pushnotification.PushNotificationUtil
                            .buildPushNotificationRenderedListenerKey(
                                pushAccountId,
                                pushId
                            )
                    )
                }
            } catch (e: JSONException) {
                core.config.logger.verbose(
                    config.accountId,
                    "Encountered an exception while parsing the push notification viewed event queue"
                )
            }
        }

        core.config.logger.verbose(
            config.accountId,
            "push notification viewed event sent successfully"
        )
    }

    private fun notifyListenerForPushImpressionSentToServer(listenerKey: String) {
        val notificationRenderedListener = CleverTapAPI.getNotificationRenderedListener(listenerKey)
        notificationRenderedListener?.onNotificationRendered(true)
    }
}
