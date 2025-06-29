package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.CTPreferenceCache.Companion.getInstance
import com.clevertap.android.sdk.StoreProvider.Companion.getInstance
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
import com.clevertap.android.sdk.inapp.InAppQueue
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
import com.clevertap.android.sdk.network.CompositeBatchListener
import com.clevertap.android.sdk.network.FetchInAppListener
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.response.InAppResponse
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

        val coreState = CoreState()

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

        coreState.storeRegistry = storeRegistry

        val coreMetaData = CoreMetaData()
        coreState.coreMetaData = coreMetaData

        val validator = Validator()

        val validationResultStack = ValidationResultStack()
        coreState.validationResultStack = validationResultStack

        val ctLockManager = CTLockManager()
        coreState.ctLockManager = ctLockManager

        val mainLooperHandler = MainLooperHandler()
        coreState.mainLooperHandler = mainLooperHandler

        val config = CleverTapInstanceConfig(cleverTapInstanceConfig)
        coreState.config = config

        val fileResourceProviderInit = CTExecutorFactory.executors(config).ioTask<Unit>()
        fileResourceProviderInit.execute("initFileResourceProvider") {
            FileResourceProvider.getInstance(context, config.logger)
        }

        val baseDatabaseManager = DBManager(config, ctLockManager)
        coreState.databaseManager = baseDatabaseManager

        val repository = CryptRepository(
            context = context,
            accountId = config.accountId
        )
        val cryptFactory = CryptFactory(
            context = context,
            accountId = config.accountId
        )
        val cryptHandler = CryptHandler(
            encryptionLevel = fromInt(value = config.encryptionLevel),
            accountID = config.accountId,
            repository = repository,
            cryptFactory = cryptFactory
        )
        coreState.cryptHandler = cryptHandler
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>()
        task.execute("migratingEncryption") {

            val dataMigrationRepository = DataMigrationRepository(
                context = context,
                config = config,
                dbAdapter = baseDatabaseManager.loadDBAdapter(context)
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
            null
        }

        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData)
        coreState.deviceInfo = deviceInfo
        deviceInfo.onInitDeviceInfo(cleverTapID)

        val localDataStore =
            LocalDataStore(context, config, cryptHandler, deviceInfo, baseDatabaseManager)
        coreState.localDataStore = localDataStore

        val profileValueHandler = ProfileValueHandler(validator, validationResultStack)
        coreState.profileValueHandler = profileValueHandler

        val eventMediator =
            EventMediator(context, config, coreMetaData, localDataStore, profileValueHandler)
        coreState.eventMediator = eventMediator

        getInstance(context, config)

        val callbackManager: BaseCallbackManager = CallbackManager(config, deviceInfo)
        coreState.callbackManager = callbackManager

        val sessionManager = SessionManager(config, coreMetaData, validator, localDataStore)
        coreState.sessionManager = sessionManager

        val controllerManager = ControllerManager(
            context,
            config,
            ctLockManager,
            callbackManager,
            deviceInfo,
            baseDatabaseManager
        )
        coreState.controllerManager = controllerManager

        val triggersMatcher = TriggersMatcher(localDataStore)
        val triggersManager = TriggerManager(context, config.accountId, deviceInfo)
        val impressionManager = ImpressionManager(storeRegistry)
        val limitsMatcher = LimitsMatcher(impressionManager, triggersManager)

        coreState.impressionManager = impressionManager

        val inAppActionHandler = InAppActionHandler(
            context,
            config,
            PushPermissionHandler(config, callbackManager.pushPermissionResponseListenerList)
        )
        val systemTemplates = SystemTemplates.getSystemTemplates(inAppActionHandler)
        val templatesManager = TemplatesManager.createInstance(config, systemTemplates)
        coreState.templatesManager = templatesManager

        val evaluationManager = EvaluationManager(
            triggersMatcher = triggersMatcher,
            triggersManager = triggersManager,
            limitsMatcher = limitsMatcher,
            storeRegistry = storeRegistry,
            templatesManager = templatesManager
        )
        coreState.evaluationManager = evaluationManager

        val taskInitStores = CTExecutorFactory.executors(config).ioTask<Void?>()
        taskInitStores.execute("initStores") {
            if (coreState.deviceInfo != null && coreState.deviceInfo.getDeviceID() != null) {
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
            null
        }

        //Get device id should be async to avoid strict mode policy.
        val taskInitFCManager = CTExecutorFactory.executors(config).ioTask<Void>()
        taskInitFCManager.execute("initFCManager") {
            if (coreState.deviceInfo != null && coreState.deviceInfo.deviceID != null && controllerManager.inAppFCManager == null) {
                coreState.config.logger
                    .verbose(
                        config.accountId + ":async_deviceID",
                        "Initializing InAppFC with device Id = " + coreState.deviceInfo
                            .deviceID
                    )
                controllerManager.inAppFCManager = InAppFCManager(
                    context,
                    config,
                    coreState.deviceInfo.deviceID,
                    storeRegistry,
                    impressionManager
                )
            }
            null
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
        coreState.varCache = varCache

        val ctVariables = CTVariables(varCache)
        coreState.ctVariables = ctVariables
        coreState.controllerManager.ctVariables = ctVariables

        val parser = Parser(ctVariables)
        coreState.parser = parser

        val taskVariablesInit = CTExecutorFactory.executors(config).ioTask<Void?>()
        taskVariablesInit.execute("initCTVariables") {
            ctVariables.init()
            null
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
            context = context,
            config = config,
            deviceInfo = deviceInfo
        )
        val networkManager = NetworkManager(
            context,
            config,
            deviceInfo,
            coreMetaData,
            validationResultStack,
            controllerManager,
            baseDatabaseManager,
            callbackManager,
            ctLockManager,
            validator,
            inAppResponse,
            ctApiWrapper
        )
        coreState.networkManager = networkManager

        val loginInfoProvider = LoginInfoProvider(
            context,
            config,
            cryptHandler
        )

        val baseEventQueueManager = EventQueueManager(
            baseDatabaseManager,
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
        coreState.baseEventQueueManager = baseEventQueueManager

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
            SYSTEM
        )
        coreState.analyticsManager = analyticsManager

        networkManager.addNetworkHeadersListener(evaluationManager)
        val inAppController = InAppController(
            context,
            config,
            mainLooperHandler,
            controllerManager,
            callbackManager,
            analyticsManager,
            coreMetaData,
            deviceInfo,
            InAppQueue(config, storeRegistry),
            evaluationManager,
            templatesManager,
            storeRegistry,
            inAppActionHandler
        )

        coreState.inAppController = inAppController
        coreState.controllerManager.inAppController = inAppController

        val appLaunchListener = AppLaunchListener()
        appLaunchListener.addListener(inAppController.onAppLaunchEventSent)

        val batchListener = CompositeBatchListener()
        batchListener.addListener(appLaunchListener)
        batchListener.addListener(FetchInAppListener(callbackManager))
        callbackManager.batchListener = batchListener

        val taskInitFeatureFlags = CTExecutorFactory.executors(config).ioTask<Void>()
        taskInitFeatureFlags.execute("initFeatureFlags") {
            initFeatureFlags(
                context,
                controllerManager,
                config,
                deviceInfo,
                callbackManager,
                analyticsManager
            )
            null
        }

        val locationManager = LocationManager(context, config, coreMetaData, baseEventQueueManager)
        coreState.locationManager = locationManager

        val ctWorkManager = CTWorkManager(context, config)

        val pushProviders = PushProviders
            .load(
                context, config, baseDatabaseManager, validationResultStack,
                analyticsManager, controllerManager, ctWorkManager
            )
        coreState.pushProviders = pushProviders

        val activityLifeCycleManager = ActivityLifeCycleManager(
            context,
            config,
            analyticsManager,
            coreMetaData,
            sessionManager,
            pushProviders,
            callbackManager,
            inAppController,
            baseEventQueueManager
        )
        coreState.activityLifeCycleManager = activityLifeCycleManager

        val loginController = LoginController(
            context, config, deviceInfo,
            validationResultStack, baseEventQueueManager, analyticsManager,
            coreMetaData, controllerManager, sessionManager,
            localDataStore, callbackManager, baseDatabaseManager, ctLockManager, loginInfoProvider
        )
        coreState.loginController = loginController

        return coreState
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