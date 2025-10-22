package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.CTPreferenceCache.Companion.getInstance
import com.clevertap.android.sdk.StoreProvider.Companion.getInstance
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.EncryptionLevel.Companion.fromInt
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventMediator
import com.clevertap.android.sdk.events.EventQueueManager
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
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
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
import com.clevertap.android.sdk.variables.repo.VariablesRepo

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
            repository = repository,
            cryptFactory = cryptFactory
        )

        val dbEncryptionHandler = DBEncryptionHandler(
            crypt = cryptHandler,
            logger = config.logger,
            encryptionLevel = fromInt(config.encryptionLevel)
        )

        val variablesRepo = VariablesRepo(
            context = context,
            accountId = config.accountId,
            dbEncryptionHandler = dbEncryptionHandler
        )

        val databaseName = DBAdapter.getDatabaseName(config)

        val databaseManager = DBManager(
            accountId = config.accountId,
            logger = config.logger,
            databaseName = databaseName,
            ctLockManager = ctLockManager,
            ijRepo = ijRepo,
            dbEncryptionHandler = dbEncryptionHandler,
            clearFirstRequestTs = networkRepo::clearFirstRequestTs,
            clearLastRequestTs = networkRepo::clearLastRequestTs
        )

        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData, executors)

        val localDataStore =
            LocalDataStore(context, config, cryptHandler, deviceInfo, databaseManager)

        val profileValueHandler = ProfileValueHandler(validator, validationResultStack)

        val eventMediator =
            EventMediator(config, coreMetaData, localDataStore, profileValueHandler, networkRepo)

        getInstance(context, config)

        val callbackManager: BaseCallbackManager = CallbackManager(config, deviceInfo)

        val sessionManager = SessionManager(config, coreMetaData, validator, localDataStore)

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

        val impl = createFileResourcesRepo(
            context = context,
            logger = config.logger,
            storeRegistry = storeRegistry
        )

        val varCache = VarCache(
            config,
            context,
            impl,
            variablesRepo
        )

        val ctVariables = CTVariables(varCache)

        val parser = Parser(ctVariables)

        val inAppResponse = InAppResponse(
            config,
            false,
            storeRegistry,
            triggersManager,
            templatesManager,
            coreMetaData,
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
            deviceInfo = deviceInfo,
            arpRepo = arpRepo,
            ijRepo = ijRepo,
            databaseManager = databaseManager,
            validationResultStack = validationResultStack,
            firstRequestTs = networkRepo::getFirstRequestTs,
            lastRequestTs = networkRepo::getLastRequestTs,
            logger = config.logger
        )

        val arpResponse = ARPResponse(config, validator, callbackManager, arpRepo)
        val contentFetchManager = ContentFetchManager(
            config,
            coreMetaData,
            queueHeaderBuilder,
            ctApiWrapper
        )
        val contentFetchResponse = ContentFetchResponse(config, contentFetchManager)
        val pushAmpResponse = PushAmpResponse(
            context,
            config,
            databaseManager,
            callbackManager
        )
        val cleverTapResponses = listOf<CleverTapResponse>(
            inAppResponse,
            MetadataResponse(config, deviceInfo, ijRepo),
            arpResponse,
            ConsoleResponse(config),
            InboxResponse(
                config,
                ctLockManager,
                callbackManager
            ),
            pushAmpResponse,
            FetchVariablesResponse(config, ctVariables, callbackManager),
            DisplayUnitResponse(config, callbackManager),
            FeatureFlagResponse(config, callbackManager),
            ProductConfigResponse(config, coreMetaData, callbackManager),
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
            databaseManager = databaseManager,
            callbackManager = callbackManager,
            ctApiWrapper = ctApiWrapper,
            encryptionManager = encryptionManager,
            arpResponse = arpResponse,
            networkRepo = networkRepo,
            queueHeaderBuilder = queueHeaderBuilder,
            cleverTapResponseHandler = responseHandler,
            ctVariables = ctVariables
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
            loginInfoProvider,
            null, // set this later
            ctVariables,
            executors
        )

        val inAppResponseForSendTestInApp = InAppResponse(
            config,
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
        baseEventQueueManager.setInAppController(inAppController)
        inAppResponse.setInAppController(inAppController)
        inAppResponseForSendTestInApp.setInAppController(inAppController)

        val batchListener = CompositeBatchListener()
        val appLaunchListener = AppLaunchListener()
        appLaunchListener.addListener(inAppController.onAppLaunchEventSent)
        batchListener.addListener(appLaunchListener)
        batchListener.addListener(FetchInAppListener(callbackManager))
        callbackManager.batchListener = batchListener

        val locationManager = LocationManager(context, config, coreMetaData, baseEventQueueManager)

        val ctWorkManager = CTWorkManager(context, config)

        val pushProviders = PushProviders(
            context,
            config,
            databaseManager,
            validationResultStack,
            analyticsManager,
            ctWorkManager,
            executors,
            SYSTEM
        )
        queueHeaderBuilder.pushProviders = pushProviders
        pushAmpResponse.setPushProviders(pushProviders)

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

        val state = CoreState(
            context = context,
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
            inAppController = inAppController,
            evaluationManager = evaluationManager,
            impressionManager = impressionManager,
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
            executors = executors,
            contentFetchManager = contentFetchManager,
            loginInfoProvider = loginInfoProvider,
            storeProvider = storeProvider,
            variablesRepository = variablesRepo
        )
        state.asyncStartup()
        return state
    }
}