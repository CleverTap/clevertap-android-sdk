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
import com.clevertap.android.sdk.events.EventGroup
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
import com.clevertap.android.sdk.features.callbacks.CoreClientCallbacks
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.product_config.CTProductConfigFactory
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.task.Task
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ManifestValidator
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal open class CoreState(
    val core: CoreFeature,
    val data: DataFeature,
    val network: NetworkFeature,
    private val analyticsProvider: () -> AnalyticsFeature,
    private val profileProvider: () -> ProfileFeature,
    private val inAppProvider: () -> InAppFeature,
    private val inboxProvider: () -> InboxFeature,
    private val variablesProvider: () -> VariablesFeature,
    private val pushProvider: () -> PushFeature,
    private val productConfigProvider: () -> ProductConfigFeature,
    private val displayUnitProvider: () -> DisplayUnitFeature,
    private val featureFlagProvider: () -> FeatureFlagFeature,
    private val geofenceProvider: () -> GeofenceFeature
) : CoreContract {

    init {
        network.coreContract = this
        core.coreContract = this
    }

    // For ease of use
    private val logger: ILogger get() = core.config.logger
    private val accountId: String get() = core.config.accountId

    val analytics: AnalyticsFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing AnalyticsFeature")
        analyticsProvider().apply { coreContract = this@CoreState }
    }

    val profileFeat: ProfileFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing ProfileFeature")
        profileProvider().apply { coreContract = this@CoreState }
    }

    val inApp: InAppFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing InAppFeature")
        inAppProvider().apply { coreContract = this@CoreState }
    }

    val inbox: InboxFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing InboxFeature")
        inboxProvider().apply { coreContract = this@CoreState }
    }

    val variables: VariablesFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing VariablesFeature")
        variablesProvider().apply { coreContract = this@CoreState }
    }

    val push: PushFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing PushFeature")
        pushProvider().apply { coreContract = this@CoreState }
    }

    val productConfig: ProductConfigFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing ProductConfigFeature")
        productConfigProvider().apply { coreContract = this@CoreState }
    }

    val displayUnitF: DisplayUnitFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing DisplayUnitFeature")
        displayUnitProvider().apply { coreContract = this@CoreState }
    }

    val featureFlagF: FeatureFlagFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing FeatureFlagFeature")
        featureFlagProvider().apply { coreContract = this@CoreState }
    }

    val geofenceF: GeofenceFeature by lazy {
        logger.info(accountId, "[LAZY] Initializing GeofenceFeature")
        geofenceProvider().apply { coreContract = this@CoreState }
    }

    fun asyncStartup() {
        val fileResourceProviderInit = core.executors.ioTask<Unit>()
        fileResourceProviderInit.execute("initFileResourceProvider") {
            FileResourceProvider.getInstance(core.context, logger)
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
                logPrefix = accountId,
                configEncryptionLevel = core.config.encryptionLevel,
                logger = logger,
                cryptHandler = core.cryptHandler,
                cryptRepository = CryptRepository(
                    context = core.context,
                    accountId = accountId
                ),
                dataMigrationRepository = dataMigrationRepository,
                variablesRepo = variables.variablesRepository,
                dbAdapter = dbAdapter
            )
            cryptMigrator.migrateEncryption()
        }

        logger.verbose(accountId + ":async_deviceID", "DeviceInfo() called")
        val taskDeviceCachedInfo = core.executors.ioTask<Unit>()
        taskDeviceCachedInfo.execute("getDeviceCachedInfo"
        ) { core.deviceInfo.getDeviceCachedInfo() }

        val task1 = core.executors.ioTask<String>()
        // callback on main thread
        task1.addOnSuccessListener { deviceId: String ->
            logger.verbose(
                accountId + ":async_deviceID",
                "DeviceID initialized successfully!" + Thread.currentThread()
            )
            // No need to put getDeviceID() on background thread because prefs already loaded
            deviceIDCreated(deviceId)
        }
        task1.execute("initDeviceID") { core.deviceInfo.initDeviceID() }

        val taskInitStores = core.executors.ioTask<Unit>()
        taskInitStores.execute("initStores") {
            inApp.initInAppStores(core.deviceInfo.getDeviceID())
        }

        //Get device id should be async to avoid strict mode policy.
        val taskInitFCManager = core.executors.ioTask<Unit>()
        taskInitFCManager.execute("initFCManager") {
            val deviceId = core.deviceInfo.deviceID
            if (deviceId != null && inApp.inAppFCManager == null) {
                logger
                    .verbose(
                        accountId + ":async_deviceID",
                        "Initializing InAppFC with device Id = $deviceId"
                    )
                inApp.initInAppFCManager(deviceId)
            }
        }

        val taskVariablesInit = core.executors.ioTask<Unit>()
        taskVariablesInit.execute("initCTVariables") {
            variables.cTVariables.init()
        }

        val taskInitFeatureFlags = core.executors.ioTask<Unit>()
        taskInitFeatureFlags.execute("initFeatureFlags") {
            featureFlagF.initialize(core.deviceInfo.deviceID)
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
            putString(core.context, accountId, "instance", configJson)
        }
        core.executors.postAsyncSafelyTask<Unit>().execute("recordDeviceIDErrors") {
            if (core.deviceInfo.getDeviceID() != null) {
                recordDeviceIDErrors()
            }
        }
    }

    fun deviceIDCreated(deviceId: String) {
        val accountId: String = accountId

        // Inflate the local profile here as deviceId is required
        data.localDataStore.inflateLocalProfileAsync(core.context)

        // must move initStores task to async executor due to addChangeUserCallback synchronization
        val task: Task<Unit> = core.executors.ioTask<Unit>()
        task.execute("initStores") {
            inApp.initInAppStores(deviceId)
        }

        /*
          Reinitialising InAppFCManager with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        if (inApp.inAppFCManager == null) {
            logger.verbose(
                "$accountId:async_deviceID",
                "Initializing InAppFC after Device ID Created = $deviceId"
            )
            inApp.initInAppFCManager(deviceId)
        }

        //todo : replace with variables
        /*
          Reinitialising product config & Feature Flag controllers with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        val ctFeatureFlagsController = featureFlagF.ctFeatureFlagsController

        if (ctFeatureFlagsController != null && TextUtils.isEmpty(ctFeatureFlagsController.getGuid())) {
            logger.verbose(
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
            logger.verbose(
                "$accountId:async_deviceID",
                "Initializing Product Config after Device ID Created = $deviceId"
            )
            ctProductConfigController.setGuidAndInit(deviceId)
        }
        logger.verbose(
            "$accountId:async_deviceID",
            "Got device id from DeviceInfo, notifying user profile initialized to SyncListener"
        )
        core.coreCallbacks.notifyCleverTapIDChanged(deviceId)
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("")
    fun getCtProductConfigController(context: Context?): CTProductConfigController? {
        if (core.config.isAnalyticsOnly) {
            core.config.getLogger()
                .debug(
                    accountId,
                    "Product Config is not enabled for this instance"
                )
            return null
        }
        if (productConfig.productConfigController == null) {
            core.config.getLogger().verbose(
                accountId + ":async_deviceID",
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
        profile: Map<String?, Any?>?,
        cacheGuid: String?,
        cleverTapID: String?
    ) {
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("resetProfile") {
            try {
                core.config.getLogger().verbose(
                    accountId,
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
                clearUserContext()

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
                variables.reset()
                analytics.analyticsManager.forcePushAppLaunchedEvent()
                if (profile != null) {
                    analytics.pushProfile(profile)
                }
                push.pushProviders.forcePushDeviceToken(true)
                inbox.reset()
                featureFlagF.reset(core.deviceInfo.getDeviceID())
                productConfig.reset()
                recordDeviceIDErrors()
                displayUnitF.reset()

                inApp.userChanged(core.deviceInfo.getDeviceID())

                inApp.inAppFCManager?.changeUser(core.deviceInfo.getDeviceID())
            } catch (t: Throwable) {
                core.config.getLogger().verbose(accountId, "Reset Profile error", t)
            }
        }
    }

    @Suppress("unused")
    fun onUserLogin(profile: Map<String?, Any?>?, cleverTapID: String?) {
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

    private fun _onUserLogin(profile: Map<String?, Any?>?, cleverTapID: String?) {
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
                val isProfileKey = key?.let { iProfileHandler.hasIdentity(it) } ?: false // todo - note we did not have this null check, it would go in catch block, please confirm
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
                        accountId,
                        "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile"
                    )
                    analytics.pushProfile(profile)
                    return
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID == currentGUID) {
                core.config.getLogger().debug(
                    accountId,
                    ("onUserLogin: " + profile + " maps to current device id " + currentGUID
                            + " pushing on current profile")
                )
                analytics.pushProfile(profile)
                return
            }

            core.config.getLogger()
                .verbose(
                    accountId, ("onUserLogin: queuing reset profile for " + profile
                            + " with Cached GUID " + (cachedGUID ?: "NULL"))
                )

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID)
        } catch (t: Throwable) {
            core.config.getLogger().verbose(accountId, "onUserLogin failed", t)
        }
    }

    /**
     * Phase 2: Delegating to InboxFeature
     */
    @AnyThread
    fun initializeInbox() {
        if (core.config.isAnalyticsOnly) {
            core.config.getLogger()
                .debug(
                    accountId,
                    "Instance is analytics only, not initializing Notification Inbox"
                )
            return
        }
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("initializeInbox") { inbox.initialize() }
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

        logger.verbose(accountId, "Processing response : $bodyJson")

        if (bodyString.isNullOrBlank() || bodyJson == null) {
            inApp.batchSent(requestBody.queue, true)
            return
        }

        // Handle decryption if needed
        val isEncryptedResponse = response.getHeaderValue(CtApi.HEADER_ENCRYPTION_ENABLED).toBoolean()

        if (isEncryptedResponse) {
            when (val decryptResponse = network.encryptionManager.decryptResponse(bodyString = bodyString)) {
                is com.clevertap.android.sdk.network.api.EncryptionFailure -> {
                    logger.verbose(accountId, "Failed to decrypt response")
                    inApp.batchSent(requestBody.queue, false)
                    return
                }
                is com.clevertap.android.sdk.network.api.EncryptionSuccess -> {
                    bodyString = decryptResponse.data
                    bodyJson = bodyString.toJsonOrNull()
                    logger.verbose("Decrypted response = $bodyString")
                }
            }
        }

        if (bodyJson == null) {
            logger.verbose("The parsed response is null so do not process.")
            return
        }

        val allFeat = listOf(core, network, analytics, inApp, inbox, variables, push, productConfig, displayUnitF, featureFlagF, geofenceF)
        val userSwitchFeats = listOf(inbox, displayUnitF, variables)

        val featuresToProcess = if (isUserSwitchFlush) userSwitchFeats else allFeat

        featuresToProcess.forEach { feat ->
            feat.handleApiData(
                response = bodyJson,
                isFullResponse = isFullResponse,
                isUserSwitching = isUserSwitchFlush
            )
        }

        // Notify success
        inApp.batchSent(batch = requestBody.queue, success = true)
    }

    override fun handleContentResponseData(
        response: JSONObject,
        isUserSwitchFlush: Boolean
    ) {

        val allFeat = listOf(core, network, analytics, inApp, inbox, variables, push, productConfig, displayUnitF, featureFlagF, geofenceF)
        allFeat.forEach { feat ->
            feat.handleApiData(response = response)
        }
    }

    override fun handleVariablesResponse(response: Response) {
        val bodyString = response.readBody()
        val bodyJson = bodyString.toJsonOrNull()

        logger.verbose(accountId, "Processing variables response : $bodyJson")

        // Process through ARP response handler
        core.arpResponse.processResponse(bodyJson, core.context, core.arpRepo)
    }

    override fun handlePushImpressionsResponse(response: Response, queue: JSONArray) {
        logger.verbose(
            accountId,
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
        inApp.evaluationManager.onSentHeaders(allHeaders, endpointId)
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
    override fun logger(): ILogger = logger
    override fun analytics(): AnalyticsManager = analytics.analyticsManager
    override fun clock(): Clock = core.clock
    override fun executors(): CTExecutors = core.executors
    override fun validationResultStack(): ValidationResultStack = core.validationResultStack
    override fun mainLooperHandler(): MainLooperHandler = core.mainLooperHandler
    override fun data(): DataFeature = data
    override fun apiWrapper(): CtApiWrapper = network.ctApiWrapper
    override fun ctLockManager(): CTLockManager = core.ctLockManager
    override fun cryptHandler(): ICryptHandler = core.cryptHandler
    override fun clientCallbacks(): CoreClientCallbacks = core.coreCallbacks

    // ============ HELPER METHODS ============

    @Throws(JSONException::class)
    private fun notifyListenersForPushImpressionSentToServer(queue: JSONArray) {
        push.notifyPushImpressionsSentToServer(queue)
    }

    // lifecycle triggers
    fun activityPaused() {
        CoreMetaData.setAppForeground(false)
        analytics.sessionManager.appLastSeen = core.clock.currentTimeMillis()
        logger.verbose(accountId, "App in background")
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("activityPaused") {
            val now = core.clock.currentTimeSecondsInt()
            if (core.coreMetaData.inCurrentSession()) {
                try {
                    StorageHelper.putInt(
                        core.context,
                        accountId,
                        Constants.LAST_SESSION_EPOCH,
                        now
                    )
                    core.config.getLogger()
                        .verbose(accountId, "Updated session time: $now")
                } catch (t: Throwable) {
                    core.config.getLogger().verbose(
                        accountId,
                        "Failed to update session time time: " + t.message
                    )
                }
            }
        }
    }

    fun activityResumed(activity: Activity?) {
        core.config.getLogger().verbose(accountId, "App in foreground")
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
                core.config.getLogger().verbose(accountId, e.localizedMessage)
            } catch (_: Exception) {
                core.config.getLogger().verbose(accountId, "Failed to trigger location")
            }
        }
        analytics.baseEventQueueManager.pushInitialEventsAsync()
        inApp.inAppController.showNotificationIfAvailable()
    }

    private fun handleInstallReferrerOnFirstInstall() {
        core.config.getLogger().verbose(accountId, "Starting to handle install referrer")
        try {
            val referrerClient = InstallReferrerClient.newBuilder(core.context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerServiceDisconnected() {
                    if (!core.coreMetaData.isInstallReferrerDataSent) {
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
                                        accountId,
                                        "Install Referrer data set [Referrer URL-$referrerUrl]"
                                    )
                                } catch (npe: NullPointerException) {
                                    core.config.getLogger().debug(
                                        accountId,
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
                                        accountId,
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
                                accountId,
                                "Install Referrer data not set, API not supported by Play Store on device"
                            )

                        // Connection couldn't be established.
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->
                            core.config.getLogger().debug(
                                accountId,
                                "Install Referrer data not set, connection to Play Store unavailable"
                            )
                    }
                }
            })
        } catch (t: Throwable) {
            logger.verbose(
                accountId,
                ("Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                    .localizedMessage
                        + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle")
            )
        }
    }

    fun handleInboxPreview(extras: Bundle) {
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("testInboxNotification") {
            inbox.handleSendTestInbox(extras)
        }
    }

    fun handleInAppPreview(extras: Bundle) {
        val task = core.executors.postAsyncSafelyTask<Unit>()
        task.execute("testInappNotification") {
            inApp.handleInAppPreview(extras)
        }
    }

    /**
     * This method handles send Test flow for Display Units
     *
     * @param extras - bundled data of notification payload
     */
    @WorkerThread
    fun handleSendTestForDisplayUnits(extras: Bundle) {
        try {
            displayUnitF.handleSendTest(extras)
        } catch (t: Throwable) {
            Logger.v("Failed to process Display Unit from push notification payload", t)
        }
    }

    /**
     * This function first checks if the notification is a test push for previewing
     * In-App messages, App Inbox messages, or Product Configs/Feature Flags/Display Units.
     * If it is, it invokes the corresponding preview handler.
     */
    fun pushNotificationClickedEvent(extras: Bundle) {
        // Check for preview types first
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
        push.handlePushNotificationClicked(extras = extras)
    }

    /**
     * Constructs the network header for a request queue.
     *
     * This function assembles a [JSONObject] containing various pieces of device, user, and session
     * information that are sent with every network request containing a queue of events.
     * It utilizes [QueueHeaderBuilder] to gather data from different parts of the SDK.
     * Additionally, it attaches headers specific to In-App notifications if available.
     */
    override fun networkHeaderForQueue(endpointId: EndpointId, caller: String?): JSONObject? {
        val header = QueueHeaderBuilder(
            context = core.context,
            config = core.config,
            coreMetaData = core.coreMetaData,
            deviceInfo = core.deviceInfo,
            arpRepo = core.arpRepo,
            ijRepo = core.ijRepo,
            databaseManager = data.databaseManager,
            validationResultStack = core.validationResultStack,
            firstRequestTs = network.networkRepo::getFirstRequestTs,
            lastRequestTs = network.networkRepo::getLastRequestTs,
            logger = logger
        ).apply {
            inAppFCManager = inApp.inAppFCManager
            pushProviders = push.pushProviders
        }.buildHeader(caller)?.apply {
            val onAttachHeaders = inApp.evaluationManager.onAttachHeaders(endpointId)
            if (onAttachHeaders != null) {
                this.copyFrom(onAttachHeaders)
            }
        }
        return header
    }

    /**
     * Evaluates and potentially displays an in-app notification based on the triggered event.
     * This method is called when an event is raised, and it checks if any in-app campaigns
     * are configured to be shown for that event.
     *
     * Delegates to InAppFeature.
     *
     * @param context The Android context
     * @param event The [JSONObject] representing the event that was triggered
     * @param eventType The type of event (RAISED_EVENT, PROFILE_EVENT, etc.)
     */
    override fun evaluateInAppForEvent(context: Context, event: JSONObject, eventType: Int) {
        inApp.evaluateInAppForEvent(
            context = context,
            event = event,
            eventType = eventType,
            eventMediator = analytics.eventMediator
        )
    }

    /**
     * Clears the user-specific data from the SDK.
     * This includes clearing all event queues from the database and resetting In-App and ARP/I-J data.
     * It's typically called when a user logs out or switches profiles.
     *
     * This method must be called on a worker thread.
     */
    @WorkerThread
    override fun clearUserContext() {
        synchronized(core.ctLockManager.eventLock) {
            data.databaseManager.clearQueues(core.context)
            core.ijRepo.clearIJ(core.context)
            network.networkRepo.clearFirstRequestTs()
            network.networkRepo.clearLastRequestTs()
        }
    }
}
