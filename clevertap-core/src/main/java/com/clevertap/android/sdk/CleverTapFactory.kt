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
import com.clevertap.android.sdk.network.ArpRepo
import com.clevertap.android.sdk.network.ContentFetchManager
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
import com.clevertap.android.sdk.features.AnalyticsFeature
import com.clevertap.android.sdk.features.CallbackFeature
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
import com.clevertap.android.sdk.features.callbacks.InAppCallbackManager

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

        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData)

        val localDataStore =
            LocalDataStore(context, config, cryptHandler, deviceInfo, databaseManager)

        val profileValueHandler = ProfileValueHandler(validator, validationResultStack)

        val eventMediator =
            EventMediator(config, coreMetaData, localDataStore, profileValueHandler, networkRepo)

        getInstance(context, config)

        val callbackManager = CallbackManager(deviceInfo)

        val sessionManager = SessionManager(config, coreMetaData, validator, localDataStore)

        val triggersMatcher = TriggersMatcher(localDataStore)
        val triggersManager = TriggerManager(context, config.accountId, deviceInfo)
        val impressionManager = ImpressionManager(storeRegistry)
        val limitsMatcher = LimitsMatcher(impressionManager, triggersManager)

        val inAppActionHandler = InAppActionHandler(
            context = context,
            ctConfig = config,
            pushPermissionHandler = PushPermissionHandler(
                config = config,
                ctListeners = callbackManager.pushPermissionResponseListenerList
            )
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

        val arpResponse = ARPResponse(validator, arpRepo)
        val contentFetchManager = ContentFetchManager(
            config,
            coreMetaData,
            queueHeaderBuilder,
            ctApiWrapper
        )
        val contentFetchResponse = ContentFetchResponse(config, contentFetchManager)
        val pushAmpResponse = PushAmpResponse(
            context,
            accountId,
            config.logger,
            databaseManager
        )
        val cleverTapResponses = listOf<CleverTapResponse>(
            MetadataResponse(config, deviceInfo, ijRepo),
            arpResponse,
            ConsoleResponse(config),
            InboxResponse(
                accountId,
                config.logger,
                ctLockManager
            ),
            pushAmpResponse,
            FetchVariablesResponse(config, ctVariables),
            DisplayUnitResponse(accountId, config.logger),
            FeatureFlagResponse(accountId, config.logger),
            ProductConfigResponse(config, coreMetaData),
            GeofenceResponse(accountId, config.logger),
            contentFetchResponse
        )

        val responseHandler = ClevertapResponseHandler(context, cleverTapResponses)
        contentFetchManager.clevertapResponseHandler = responseHandler

        val networkManager = NetworkManager(
            ctApiWrapper = ctApiWrapper,
            encryptionManager = encryptionManager,
            networkRepo = networkRepo,
            queueHeaderBuilder = queueHeaderBuilder
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
            mainLooperHandler,
            deviceInfo,
            validationResultStack,
            networkManager,
            coreMetaData,
            ctLockManager,
            localDataStore,
            loginInfoProvider,
            null, // set this later
            executors
        )

        val analyticsManager = AnalyticsManager(
            context,
            config,
            baseEventQueueManager,
            validator,
            validationResultStack,
            coreMetaData,
            deviceInfo,
            ctLockManager,
            null, // todo fixme
            SYSTEM,
            executors
        )

        val inAppNotificationInflater = InAppNotificationInflater(
            storeRegistry,
            templatesManager,
            executors,
            { FileResourceProvider.getInstance(context, config.logger) }
        )

        val inAppCallbackManager = InAppCallbackManager()
        val inAppController = InAppController(
            context,
            config,
            executors,
            inAppCallbackManager,
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

        // ========== Build Feature Groups ==========
        
        // Core infrastructure
        val coreFeature = CoreFeature(
            context = context,
            config = config,
            deviceInfo = deviceInfo,
            coreMetaData = coreMetaData,
            executors = executors,
            mainLooperHandler = mainLooperHandler,
            validationResultStack = validationResultStack,
            cryptHandler = cryptHandler,
            clock = SYSTEM,
            arpResponse = arpResponse
        )
        
        // Data layer
        val dataFeature = DataFeature(
            databaseManager = databaseManager,
            localDataStore = localDataStore,
            storeRegistry = storeRegistry,
            storeProvider = storeProvider
        )
        
        // Network layer
        val networkFeature = NetworkFeature(
            networkManager = networkManager,
            contentFetchManager = contentFetchManager,
            encryptionManager = encryptionManager,
            arpResponse = arpResponse,
            clevertapResponseHandler = responseHandler,
            networkHeadersListeners = mutableListOf()
        )
        networkFeature.addNetworkHeadersListener(evaluationManager)

        // Analytics
        val analyticsFeature = AnalyticsFeature(
            analyticsManager = analyticsManager,
            baseEventQueueManager = baseEventQueueManager,
            eventMediator = eventMediator,
            sessionManager = sessionManager
        )
        
        // Profile
        val profileFeature = ProfileFeature(
            loginInfoProvider = loginInfoProvider,
            profileValueHandler = profileValueHandler,
            locationManager = locationManager
        )
        
        // InApp
        val inAppFeature = InAppFeature(
            inAppController = inAppController,
            evaluationManager = evaluationManager,
            impressionManager = impressionManager,
            templatesManager = templatesManager,
            inAppResponse = InAppResponse(accountId, config.logger, false, templatesManager),
            inAppCallbackManager = inAppCallbackManager,
            storeRegistry = storeRegistry,
            executors = executors,
            triggerManager = triggersManager
        )
        
        // Inbox
        val inboxFeature = InboxFeature(
            cTLockManager = ctLockManager,
            inboxResponse = InboxResponse(
                accountId,
                config.logger,
                ctLockManager
            )
        )
        
        // Variables
        val variablesFeature = VariablesFeature(
            cTVariables = ctVariables,
            varCache = varCache,
            parser = parser,
            variablesRepository = variablesRepo,
            fetchVariablesResponse = FetchVariablesResponse(config, ctVariables)
        )
        
        // Push
        val pushFeature = PushFeature(
            pushProviders = pushProviders,
            pushAmpResponse = pushAmpResponse
        )

        // Callback
        val callbackFeature = CallbackFeature(
            callbackManager = callbackManager
        )
        // DisplayUnit
        val displayUnitFeature = DisplayUnitFeature(
            contentFetchManager = contentFetchManager,
            displayUnitResponse = DisplayUnitResponse(accountId, config.logger)
        )

        // Geofence
        val geofenceFeature = GeofenceFeature(
            locationManager = locationManager,
            geofenceResponse = GeofenceResponse(accountId, config.logger)
        )

        // FeatureFlag
        val featureFlagFeature = FeatureFlagFeature(
            featureFlagResponse = FeatureFlagResponse(accountId, config.logger)
        )
        
        // ========== Create CoreState with Feature Groups ==========
        val state = CoreState(
            core = coreFeature,
            data = dataFeature,
            network = networkFeature,
            analytics = analyticsFeature,
            profileFeat = profileFeature,
            inApp = inAppFeature,
            inbox = inboxFeature,
            variables = variablesFeature,
            push = pushFeature,
            callback = callbackFeature,
            productConfig = ProductConfigFeature(
                productConfigResponse = ProductConfigResponse(config, coreMetaData),
                arpResponse = arpResponse
            ),
            displayUnitF = displayUnitFeature,
            featureFlagF = featureFlagFeature,
            geofenceF = geofenceFeature
        )
        state.asyncStartup()
        return state
    }
}