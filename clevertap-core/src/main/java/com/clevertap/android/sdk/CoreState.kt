package com.clevertap.android.sdk

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
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
import com.clevertap.android.sdk.features.CoreFeature
import com.clevertap.android.sdk.features.DataFeature
import com.clevertap.android.sdk.features.DisplayUnitFeature
import com.clevertap.android.sdk.features.FeatureFlagFeature
import com.clevertap.android.sdk.features.GeofenceFeature
import com.clevertap.android.sdk.features.InAppFeature
import com.clevertap.android.sdk.features.InboxFeature
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
    val productConfig: ProductConfigFeature,
    val displayUnitF: DisplayUnitFeature,
    val featureFlagF: FeatureFlagFeature,
    val geofenceF: GeofenceFeature
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
    val analyticsManager: AnalyticsManager get() = analytics.analyticsManager
    val baseEventQueueManager: BaseEventQueueManager get() = analytics.baseEventQueueManager
    val cTLockManager: CTLockManager get() = inbox.cTLockManager
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
    val contentFetchManager: ContentFetchManager get() = displayUnitF.contentFetchManager
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
                context = core.context,
                config = core.config,
                deviceInfo = core.deviceInfo,
                analyticsManager = analytics.analyticsManager
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
            }
            if (data.storeRegistry.impressionStore == null) {
                val impStore: ImpressionStore = data.storeProvider.provideImpressionStore(
                    context = core.context,
                    deviceId = deviceId,
                    accountId = core.config.accountId
                )
                data.storeRegistry.impressionStore = impStore
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
        core.coreCallbacks.notifyCleverTapIDChanged(deviceId)
    }

    private fun initFeatureFlags(
        context: Context?,
        config: CleverTapInstanceConfig,
        deviceInfo: DeviceInfo,
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
                config, analyticsManager
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
                    context,
                    core.deviceInfo,
                    core.config,
                    analytics.analyticsManager,
                    core.coreMetaData,
                    productConfig.callbacks
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
                displayUnitF.contentFetchManager.cancelAllResponseJobs()
                data.databaseManager.clearQueues(core.context)

                // clear out the old data
                CoreMetaData.setActivityCount(1)
                analytics.sessionManager.destroySession()

                // either force restore the cached GUID or generate a new one
                if (cacheGuid != null) {
                    core.deviceInfo.forceUpdateDeviceId(cacheGuid)
                } else if (core.config.enableCustomCleverTapId) {
                    core.deviceInfo.forceUpdateCustomCleverTapID(cleverTapID)
                } else {
                    core.deviceInfo.forceNewDeviceID()
                }

                data.localDataStore.changeUser()

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

                inApp.userChanged(core.deviceInfo.getDeviceID())

                inApp.inAppFCManager?.changeUser(core.deviceInfo.getDeviceID())
            } catch (t: Throwable) {
                core.config.getLogger().verbose(core.config.accountId, "Reset Profile error", t)
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
                core.context,
                core.deviceInfo,
                core.config,
                analytics.analyticsManager,
                core.coreMetaData,
                productConfig.callbacks
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
            inApp.batchSent(requestBody.queue, true)
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
                    inApp.batchSent(requestBody.queue, false)
                    return
                }
                is com.clevertap.android.sdk.network.api.EncryptionSuccess -> {
                    bodyString = decryptResponse.data
                    bodyJson = bodyString.toJsonOrNull()
                    core.config.logger.verbose("Decrypted response = $bodyString")
                }
            }
        }

        if (bodyJson == null) {
            core.config.logger.verbose("The parsed response is null so do not process.")
            return
        }

        val allFeat = listOf(core, network, analytics, inApp, inbox, variables, push, productConfig, displayUnitF, featureFlagF, geofenceF)
        val userSwitchFeats = listOf(inbox, displayUnitF, variables)

        val featuresToProcess = if (isUserSwitchFlush) userSwitchFeats else allFeat

        featuresToProcess.forEach { feat ->
            feat.handleApiData(bodyJson, bodyString, context)
        }

        // Notify success
        inApp.batchSent(requestBody.queue, true)
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
        inApp.batchSent(queue, success)
    }

    override fun onFlushFailure(context: Context) {
        analytics.networkFailed()
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
        core.coreCallbacks.scDomainListener?.onSCDomainAvailable(domain)
    }

    override fun notifySCDomainUnavailable() {
        core.coreCallbacks.scDomainListener?.onSCDomainUnavailable()
    }

    override fun didNotFlush() {
        variables.invokeCallbacksForNetworkError()
        inApp.batchSent(JSONArray(), false)
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

    // lifecycle triggers
    fun activityPaused() {
        CoreMetaData.setAppForeground(false)
        analytics.sessionManager.appLastSeen = clock.currentTimeMillis()
        core.config.logger.verbose(core.config.accountId, "App in background")
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("activityPaused") {
            val now = core.clock.currentTimeSecondsInt()
            if (core.coreMetaData.inCurrentSession()) {
                try {
                    StorageHelper.putInt(
                        context,
                        config.accountId,
                        Constants.LAST_SESSION_EPOCH,
                        now
                    )
                    core.config.getLogger()
                        .verbose(core.config.accountId, "Updated session time: $now")
                } catch (t: Throwable) {
                    core.config.getLogger().verbose(
                        core.config.accountId,
                        "Failed to update session time time: " + t.message
                    )
                }
            }
        }
    }

    fun activityResumed(activity: Activity?) {
        core.config.getLogger().verbose(core.config.accountId, "App in foreground")
        analytics.sessionManager.checkTimeoutSession()

        //Anything in this If block will run once per App Launch.
        if (!core.coreMetaData.isAppLaunchPushed) {
            analytics.analyticsManager.pushAppLaunchedEvent()
            analytics.analyticsManager.fetchFeatureFlags()
            push.pushProviders.onTokenRefresh()
            val task = core.executors.postAsyncSafelyTask<Unit>()
            task.execute("HandlingInstallReferrer") {
                if (!core.coreMetaData.isInstallReferrerDataSent && core.coreMetaData.isFirstSession) {
                    handleInstallReferrerOnFirstInstall()
                }
            }

            val cleanUpTask = core.executors.ioTask<Unit>()
            cleanUpTask.execute("CleanUpOldGIFs") {
                Utils.cleanupOldGIFs(core.context, core.config, core.clock)
            }

            try {
                geofenceF.geofenceCallback?.triggerLocation()
            } catch (e: IllegalStateException) {
                core.config.getLogger().verbose(core.config.accountId, e.localizedMessage)
            } catch (_: Exception) {
                core.config.getLogger().verbose(core.config.accountId, "Failed to trigger location")
            }
        }
        analytics.baseEventQueueManager.pushInitialEventsAsync()
        inApp.inAppController.showNotificationIfAvailable()
    }

    private fun handleInstallReferrerOnFirstInstall() {
        core.config.getLogger().verbose(core.config.accountId, "Starting to handle install referrer")
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerServiceDisconnected() {
                    if (!coreMetaData.isInstallReferrerDataSent) {
                        handleInstallReferrerOnFirstInstall()
                    }
                }

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established
                            val task = core.executors.postAsyncSafelyTask<ReferrerDetails?>()

                            task.addOnSuccessListener { response: ReferrerDetails? ->
                                try {
                                    val referrerUrl = response!!.installReferrer
                                    core.coreMetaData.referrerClickTime =
                                        response.referrerClickTimestampSeconds
                                    core.coreMetaData.appInstallTime =
                                        response.installBeginTimestampSeconds
                                    analytics.analyticsManager.pushInstallReferrer(referrerUrl)
                                    core.coreMetaData.isInstallReferrerDataSent = true
                                    core.config.getLogger().debug(
                                        core.config.accountId,
                                        "Install Referrer data set [Referrer URL-$referrerUrl]"
                                    )
                                } catch (npe: NullPointerException) {
                                    core.config.getLogger().debug(
                                        core.config.accountId,
                                        "Install referrer client null pointer exception caused by Google Play Install Referrer library - "
                                                + npe
                                            .message
                                    )
                                    referrerClient.endConnection()
                                    core.coreMetaData.isInstallReferrerDataSent = false
                                }
                            }

                            task.execute("ActivityLifeCycleManager#getInstallReferrer") {
                                var response: ReferrerDetails? = null
                                try {
                                    response = referrerClient.installReferrer
                                } catch (e: RemoteException) {
                                    core.config.getLogger().debug(
                                        core.config.accountId,
                                        "Remote exception caused by Google Play Install Referrer library - " + e
                                            .message
                                    )
                                    referrerClient.endConnection()
                                    core.coreMetaData.isInstallReferrerDataSent = false
                                }
                                response
                            }
                        }

                        // API not available on the current Play Store app.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            core.config.getLogger().debug(
                                core.config.accountId,
                                "Install Referrer data not set, API not supported by Play Store on device"
                            )

                        // Connection couldn't be established.
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->
                            core.config.getLogger().debug(
                                core.config.accountId,
                                "Install Referrer data not set, connection to Play Store unavailable"
                            )
                    }
                }
            })
        } catch (t: Throwable) {
            config.getLogger().verbose(
                config.accountId,
                ("Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                    .localizedMessage
                        + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle")
            )
        }
    }

    fun handleInboxPreview(extras: Bundle) {
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("testInboxNotification") {
            inbox.handleSendTestInbox(extras)
        }
    }

    fun handleInAppPreview(extras: Bundle) {
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("testInappNotification") {
            inApp.handleInAppPreview(extras)
        }
    }

    /**
     * This method handles send Test flow for Display Units
     *
     * @param extras - bundled data of notification payload
     */
    fun handleSendTestForDisplayUnits(extras: Bundle) {
        try {
            displayUnitF.handleSendTest(extras)
        } catch (t: Throwable) {
            Logger.v("Failed to process Display Unit from push notification payload", t)
        }
    }

    fun pushNotificationClickedEvent(extras: Bundle) {
        if (extras.containsKey(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)) {
            handleInAppPreview(extras)
            return
        }

        if (extras.containsKey(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)) {
            handleInboxPreview(extras)
            return
        }

        if (extras.containsKey(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)) {
            handleSendTestForDisplayUnits(extras)
            return
        }
        val sent: Boolean = analyticsManager.pushNotificationClickedEvent(extras)
        if (sent) {
            push.pushNotificationListener?.onNotificationClickedPayloadReceived(Utils.convertBundleObjectToHashMap(extras))
        }
    }

    override fun clock(): Clock = core.clock
}
