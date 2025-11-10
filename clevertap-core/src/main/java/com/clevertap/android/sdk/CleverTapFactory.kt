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

import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkRepo

import com.clevertap.android.sdk.response.FeatureFlagResponse
import com.clevertap.android.sdk.response.GeofenceResponse
import com.clevertap.android.sdk.response.InboxResponse
import com.clevertap.android.sdk.response.ProductConfigResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.Clock.Companion.SYSTEM
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator

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
        
        val config = CleverTapInstanceConfig(cleverTapInstanceConfig)
        val accountId = config.accountId

        // ========== Core Infrastructure ==========
        val coreMetaData = CoreMetaData()
        val validator = Validator()
        val validationResultStack = ValidationResultStack()
        val ctLockManager = CTLockManager()
        val mainLooperHandler = MainLooperHandler()
        val executors = CTExecutorFactory.executors(config)
        
        val repository = CryptRepository(
            context = context,
            accountId = accountId
        )
        val ctKeyGenerator = CTKeyGenerator(cryptRepository = repository)
        val cryptFactory = CryptFactory(
            accountId = accountId,
            ctKeyGenerator = ctKeyGenerator
        )
        val cryptHandler = CryptHandler(
            repository = repository,
            cryptFactory = cryptFactory
        )
        
        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData)
        
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
            ctLockManager = ctLockManager
        )

        // ========== Network Layer (Created Early) ==========
        // NetworkRepo is shared between Network and Data layers
        val networkRepo = NetworkRepo(context = context, config = config)

        // Network layer (Self-Contained)
        val networkFeature = NetworkFeature(
            networkRepo = networkRepo,
            ctKeyGenerator = ctKeyGenerator,
            cryptFactory = cryptFactory
        )

        // ========== Data Layer ==========
        val storeProvider = getInstance()
        
        val storeRegistry = StoreRegistry(
            inAppStore = null,
            impressionStore = null,
            legacyInAppStore = storeProvider.provideLegacyInAppStore(context = context, accountId = accountId),
            inAppAssetsStore = storeProvider.provideInAppAssetsStore(context, accountId),
            filesStore = storeProvider.provideFileStore(context, accountId)
        )

        val dbEncryptionHandler = DBEncryptionHandler(
            crypt = cryptHandler,
            logger = config.logger,
            encryptionLevel = fromInt(config.encryptionLevel)
        )

        val databaseName = DBAdapter.getDatabaseName(config)

        val databaseManager = DBManager(
            accountId = accountId,
            logger = config.logger,
            databaseName = databaseName,
            ctLockManager = ctLockManager,
            dbEncryptionHandler = dbEncryptionHandler
        )

        val localDataStore = LocalDataStore(context, config, cryptHandler, deviceInfo, databaseManager)
        
        // Data layer
        val dataFeature = DataFeature(
            databaseManager = databaseManager,
            localDataStore = localDataStore
        )

        // ========== Analytics Layer ==========
        val profileValueHandler = ProfileValueHandler(validator, validationResultStack)

        getInstance(context, config)

        val loginInfoProvider = LoginInfoProvider(
            context,
            config,
            cryptHandler
        )

        // Analytics (Self-Contained)
        val analyticsFeature = AnalyticsFeature(
            networkFeature = networkFeature,
            validator = validator,
            profileValueHandler = profileValueHandler,
            loginInfoProvider = loginInfoProvider,
            ctLockManager = ctLockManager
        )
        
        // ========== Profile Layer ==========
        val locationManager = LocationManager(context, config, coreMetaData, analyticsFeature.baseEventQueueManager)
        
        // Profile
        val profileFeature = ProfileFeature(
            loginInfoProvider = loginInfoProvider,
            profileValueHandler = profileValueHandler,
            locationManager = locationManager
        )
        
        // ========== InApp Feature (Self-Contained) ==========
        val inAppFeature = InAppFeature(
            dataFeature = dataFeature,
            storeRegistry = storeRegistry,
            storeProvider = storeProvider
        )
        
        // ========== Other Features ==========
        
        // Inbox
        val inboxFeature = InboxFeature(
            cTLockManager = ctLockManager,
            inboxResponse = InboxResponse(
                accountId,
                config.logger,
                ctLockManager
            )
        )
        
        // Variables (Self-Contained)
        val variablesFeature = VariablesFeature(
            storeRegistry = storeRegistry,
            dbEncryptionHandler = dbEncryptionHandler
        )
        
        // Push (Self-Contained)
        val pushFeature = PushFeature()

        // DisplayUnit (Self-Contained)
        val displayUnitFeature = DisplayUnitFeature()

        // Geofence
        val geofenceFeature = GeofenceFeature(
            locationManager = locationManager,
            geofenceResponse = GeofenceResponse(accountId, config.logger)
        )

        // FeatureFlag
        val featureFlagFeature = FeatureFlagFeature(
            featureFlagResponse = FeatureFlagResponse(accountId, config.logger)
        )

        // ProductConfig
        val productConfigFeature = ProductConfigFeature(
            productConfigResponse = ProductConfigResponse(accountId, config.logger)
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
            productConfig = productConfigFeature,
            displayUnitF = displayUnitFeature,
            featureFlagF = featureFlagFeature,
            geofenceF = geofenceFeature
        )
        state.asyncStartup()
        return state
    }
}