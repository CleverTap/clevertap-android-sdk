package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.CTPreferenceCache.Companion.getInstance
import com.clevertap.android.sdk.StoreProvider.Companion.getInstance
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptMigrator
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.DataMigrationRepository
import com.clevertap.android.sdk.cryption.EncryptionLevel.Companion.fromInt
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsFactory
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.InAppNotificationInflater
import com.clevertap.android.sdk.inapp.StoreRegistryInAppQueue
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.customtemplates.system.SystemTemplates
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.evaluation.LimitsMatcher
import com.clevertap.android.sdk.inapp.evaluation.TriggersMatcher
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory.Companion.createFileResourcesRepo
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.login.LoginController
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.AppLaunchListener
import com.clevertap.android.sdk.network.ArpRepo
import com.clevertap.android.sdk.network.CompositeBatchListener
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.FetchInAppListener
import com.clevertap.android.sdk.network.IJRepo
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.CleverTapResponse
import com.clevertap.android.sdk.response.ClevertapResponseHandler
import com.clevertap.android.sdk.response.ConsoleResponse
import com.clevertap.android.sdk.response.ContentFetchResponse
import com.clevertap.android.sdk.response.DisplayUnitResponse
import com.clevertap.android.sdk.response.FeatureFlagResponse
import com.clevertap.android.sdk.response.FetchVariablesResponse
import com.clevertap.android.sdk.response.GeofenceResponse
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.response.InboxResponse
import com.clevertap.android.sdk.response.MetadataResponse
import com.clevertap.android.sdk.response.ProductConfigResponse
import com.clevertap.android.sdk.response.PushAmpResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.Clock.Companion.SYSTEM
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache

internal object CleverTapFactory {
    @JvmStatic
    fun getCoreState(
        context: Context?,
        cleverTapInstanceConfig: CleverTapInstanceConfig?,
        cleverTapID: String?
    ): CoreState {

        if (context == null || cleverTapInstanceConfig == null) {
            // todo this needs to be fixed with kotlin usage+using kotlin testing libs
            throw RuntimeException("This is invalid case and will not happen. Context/Config is null")
        }
        // create storeRegistry, preferences for features
        val storeProvider = getInstance()
        val accountId = cleverTapInstanceConfig.accountId

        val storeRegistry = StoreRegistry(
            inAppStore = null,
            impressionStore = null,
            legacyInAppStore = storeProvider.provideLegacyInAppStore(context = context, accountId = accountId),
            inAppAssetsStore = storeProvider.provideInAppAssetsStore(context, accountId),
            filesStore = storeProvider.provideFileStore(context, accountId)
        )

        val coreMetaData = CoreMetaData()
        val validator = Validator()
        val validationResultStack = ValidationResultStack()
        val ctLockManager = CTLockManager()
        val mainLooperHandler = MainLooperHandler()
        val config = CleverTapInstanceConfig(cleverTapInstanceConfig)
        val networkRepo = NetworkRepo(context = context, config = config)
        val ijRepo = IJRepo(config = config)
        val executors = CTExecutorFactory.executors(config)

        val fileResourceProviderInit = executors.ioTask<Unit>()
        fileResourceProviderInit.execute("initFileResourceProvider") {
            FileResourceProvider.getInstance(context, config.logger)
        }

        val databaseManager = DBManager(
            config = config,
            ctLockManager = ctLockManager,
            ijRepo = ijRepo,
            clearFirstRequestTs = networkRepo::clearFirstRequestTs,
            clearLastRequestTs = networkRepo::clearLastRequestTs
        )
        val repository = CryptRepository(
            context = context,
            accountId = config.accountId
        )
        val ctKeyGenerator = CTKeyGenerator(cryptRepository = repository)
        val cryptFactory = CryptFactory(
            accountId = config.accountId,
            ctKeyGenerator = ctKeyGenerator
        )
        val cryptHandler = CryptHandler(
            encryptionLevel = fromInt(value = config.encryptionLevel),
            accountID = config.accountId,
            repository = repository,
            cryptFactory = cryptFactory
        )
        val task = executors.postAsyncSafelyTask<Unit>()
        task.execute("migratingEncryption") {

            val dataMigrationRepository = DataMigrationRepository(
                context = context,
                config = config,
                dbAdapter = databaseManager.loadDBAdapter(context)
            )

            val cryptMigrator = CryptMigrator(
                logPrefix = config.accountId,
                configEncryptionLevel = config.encryptionLevel,
                logger = config.logger,
                cryptHandler = cryptHandler,
                cryptRepository = repository,
                dataMigrationRepository = dataMigrationRepository
            )
            cryptMigrator.migrateEncryption()
        }

        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData)
        deviceInfo.onInitDeviceInfo(cleverTapID)

        val localDataStore =
            LocalDataStore(context, config, cryptHandler, deviceInfo, databaseManager)

        val profileValueHandler = ProfileValueHandler(validator, validationResultStack)

        val eventMediator =
            EventMediator(config, coreMetaData, localDataStore, profileValueHandler, networkRepo)

        getInstance(context, config)

        val callbackManager: BaseCallbackManager = CallbackManager(config, deviceInfo)

        val sessionManager = SessionManager(config, coreMetaData, validator, localDataStore)

        val controllerManager = ControllerManager(
            context,
            config,
            ctLockManager,
            callbackManager,
            deviceInfo,
            databaseManager
        )

        val triggersMatcher = TriggersMatcher(localDataStore)
        val triggersManager = TriggerManager(context, config.accountId, deviceInfo)
        val impressionManager = ImpressionManager(storeRegistry)
        val limitsMatcher = LimitsMatcher(impressionManager, triggersManager)


        val inAppActionHandler = InAppActionHandler(
            context,
            config,
            PushPermissionHandler(config, callbackManager.pushPermissionResponseListenerList)
        )
        val systemTemplates = SystemTemplates.getSystemTemplates(inAppActionHandler)
        val templatesManager = TemplatesManager.createInstance(config, systemTemplates)

        val evaluationManager = EvaluationManager(
            triggersMatcher = triggersMatcher,
            triggersManager = triggersManager,
            limitsMatcher = limitsMatcher,
            storeRegistry = storeRegistry,
            templatesManager = templatesManager
        )

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
            if (deviceId != null && controllerManager.inAppFCManager == null) {
                config.logger
                    .verbose(
                        config.accountId + ":async_deviceID",
                        "Initializing InAppFC with device Id = $deviceId"
                    )
                controllerManager.inAppFCManager = InAppFCManager(
                    context,
                    config,
                    deviceId,
                    storeRegistry,
                    impressionManager,
                    executors,
                    SYSTEM
                )
            }
        }

        val impl = createFileResourcesRepo(
            context = context,
            logger = config.logger,
            storeRegistry = storeRegistry
        )

        val varCache = VarCache(
            config,
            context,
            impl
        )

        val ctVariables = CTVariables(varCache)
        controllerManager.ctVariables = ctVariables

        val parser = Parser(ctVariables)

        val taskVariablesInit = executors.ioTask<Unit>()
        taskVariablesInit.execute("initCTVariables") {
            ctVariables.init()
        }

        val inAppResponse = InAppResponse(
            config,
            controllerManager,
            false,
            storeRegistry,
            triggersManager,
            templatesManager,
            coreMetaData
        )

        val ctApiWrapper = CtApiWrapper(
            networkRepo = networkRepo,
            config = config,
            deviceInfo = deviceInfo
        )
        val encryptionManager = NetworkEncryptionManager(
            keyGenerator = ctKeyGenerator,
            aesgcm = cryptFactory.getAesGcmCrypt()
        )
        val arpRepo = ArpRepo(
            accountId = config.accountId,
            logger = config.logger,
            deviceInfo = deviceInfo
        )
        val queueHeaderBuilder = QueueHeaderBuilder(
            context = context,
            config = config,
            coreMetaData = coreMetaData,
            controllerManager = controllerManager,
            deviceInfo = deviceInfo,
            arpRepo = arpRepo,
            ijRepo = ijRepo,
            databaseManager = databaseManager,
            validationResultStack = validationResultStack,
            firstRequestTs = networkRepo::getFirstRequestTs,
            lastRequestTs = networkRepo::getLastRequestTs,
            logger = config.logger
        )

        val arpResponse = ARPResponse(config, validator, controllerManager, arpRepo)
        val contentFetchManager = ContentFetchManager(
            config,
            coreMetaData,
            queueHeaderBuilder,
            ctApiWrapper
        )
        val contentFetchResponse = ContentFetchResponse(config, contentFetchManager)
        val cleverTapResponses = listOf<CleverTapResponse>(
            inAppResponse,
            MetadataResponse(config, deviceInfo, ijRepo),
            arpResponse,
            ConsoleResponse(config),
            InboxResponse(
                config, ctLockManager,
                callbackManager,
                controllerManager
            ),
            PushAmpResponse(
                context,
                config,
                databaseManager,
                callbackManager,
                controllerManager
            ),
            FetchVariablesResponse(config, controllerManager, callbackManager),
            DisplayUnitResponse(config, callbackManager, controllerManager),
            FeatureFlagResponse(config, controllerManager),
            ProductConfigResponse(config, coreMetaData, controllerManager),
            GeofenceResponse(config, callbackManager),
            contentFetchResponse
        )

        val responseHandler = ClevertapResponseHandler(context, cleverTapResponses)
        contentFetchManager.clevertapResponseHandler = responseHandler

        val networkManager = NetworkManager(
            context = context,
            config = config,
            deviceInfo = deviceInfo,
            coreMetaData = coreMetaData,
            controllerManager = controllerManager,
            databaseManager = databaseManager,
            callbackManager = callbackManager,
            ctApiWrapper = ctApiWrapper,
            encryptionManager = encryptionManager,
            arpResponse = arpResponse,
            networkRepo = networkRepo,
            queueHeaderBuilder = queueHeaderBuilder,
            cleverTapResponseHandler = responseHandler
        )

        val loginInfoProvider = LoginInfoProvider(
            context,
            config,
            cryptHandler
        )

        val baseEventQueueManager = EventQueueManager(
            databaseManager,
            context,
            config,
            eventMediator,
            sessionManager,
            callbackManager,
            mainLooperHandler,
            deviceInfo,
            validationResultStack,
            networkManager,
            coreMetaData,
            ctLockManager,
            localDataStore,
            controllerManager,
            loginInfoProvider
        )

        val inAppResponseForSendTestInApp = InAppResponse(
            config,
            controllerManager,
            true,
            storeRegistry,
            triggersManager,
            templatesManager,
            coreMetaData
        )

        val analyticsManager = AnalyticsManager(
            context,
            config,
            baseEventQueueManager,
            validator,
            validationResultStack,
            coreMetaData,
            deviceInfo,
            callbackManager,
            controllerManager,
            ctLockManager,
            inAppResponseForSendTestInApp,
            SYSTEM,
            executors
        )

        val inAppNotificationInflater = InAppNotificationInflater(
            storeRegistry,
            templatesManager,
            executors,
            { FileResourceProvider.getInstance(context, config.logger) }
        )

        networkManager.addNetworkHeadersListener(evaluationManager)
        val inAppController = InAppController(
            context,
            config,
            executors,
            controllerManager,
            callbackManager,
            analyticsManager,
            coreMetaData,
            ManifestInfo.getInstance(context),
            deviceInfo,
            StoreRegistryInAppQueue(storeRegistry, config.accountId),
            evaluationManager,
            templatesManager,
            inAppActionHandler,
            inAppNotificationInflater,
            SYSTEM
        )
        controllerManager.inAppController = inAppController

        val appLaunchListener = AppLaunchListener()
        appLaunchListener.addListener(inAppController.onAppLaunchEventSent)

        val batchListener = CompositeBatchListener()
        batchListener.addListener(appLaunchListener)
        batchListener.addListener(FetchInAppListener(callbackManager))
        callbackManager.batchListener = batchListener

        val taskInitFeatureFlags = executors.ioTask<Unit>()
        taskInitFeatureFlags.execute("initFeatureFlags") {
            initFeatureFlags(
                context,
                controllerManager,
                config,
                deviceInfo,
                callbackManager,
                analyticsManager
            )
        }

        val locationManager = LocationManager(context, config, coreMetaData, baseEventQueueManager)

        val ctWorkManager = CTWorkManager(context, config)

        val pushProviders = PushProviders
            .load(
                context, config, databaseManager, validationResultStack,
                analyticsManager, controllerManager, ctWorkManager
            )

        val activityLifeCycleManager = ActivityLifeCycleManager(
            context,
            config,
            analyticsManager,
            coreMetaData,
            sessionManager,
            pushProviders,
            callbackManager,
            inAppController,
            baseEventQueueManager,
            executors,
            SYSTEM
        )

        val loginController = LoginController(
            context, config, deviceInfo,
            validationResultStack, baseEventQueueManager, analyticsManager,
            coreMetaData, controllerManager, sessionManager,
            localDataStore, callbackManager, databaseManager, ctLockManager, loginInfoProvider, contentFetchManager
        )

        return CoreState(
            locationManager = locationManager,
            config = config,
            coreMetaData = coreMetaData,
            databaseManager = databaseManager,
            deviceInfo = deviceInfo,
            eventMediator = eventMediator,
            localDataStore = localDataStore,
            activityLifeCycleManager = activityLifeCycleManager,
            analyticsManager = analyticsManager,
            baseEventQueueManager = baseEventQueueManager,
            cTLockManager = ctLockManager,
            callbackManager = callbackManager,
            controllerManager = controllerManager,
            inAppController = inAppController,
            evaluationManager = evaluationManager,
            impressionManager = impressionManager,
            loginController = loginController,
            sessionManager = sessionManager,
            validationResultStack = validationResultStack,
            mainLooperHandler = mainLooperHandler,
            networkManager = networkManager,
            pushProviders = pushProviders,
            varCache = varCache,
            parser = parser,
            cryptHandler = cryptHandler,
            storeRegistry = storeRegistry,
            templatesManager = templatesManager,
            profileValueHandler = profileValueHandler,
            cTVariables = ctVariables,
            executors = executors
        )
    }

    private fun initFeatureFlags(
        context: Context?,
        controllerManager: ControllerManager,
        config: CleverTapInstanceConfig,
        deviceInfo: DeviceInfo,
        callbackManager: BaseCallbackManager?,
        analyticsManager: AnalyticsManager?
    ) {
        config.logger.verbose(
            config.accountId + ":async_deviceID",
            "Initializing Feature Flags with device Id = " + deviceInfo.deviceID
        )
        if (config.isAnalyticsOnly) {
            config.logger.debug(config.accountId, "Feature Flag is not enabled for this instance")
        } else {
            controllerManager.ctFeatureFlagsController = CTFeatureFlagsFactory.getInstance(
                context,
                deviceInfo.deviceID,
                config, callbackManager, analyticsManager
            )
            config.logger.verbose(config.accountId + ":async_deviceID", "Feature Flags initialized")
        }
    }
}