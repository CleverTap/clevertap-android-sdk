package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptRepository
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DBManager

import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.NetworkRepo

import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MainLooperHandler
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
            throw IllegalArgumentException("Context and Config cannot be null")
        }

        val config = CleverTapInstanceConfig(cleverTapInstanceConfig)
        val accountId = config.accountId

        config.logger.info(accountId, "Creating CleverTap core components...")

        // ========== CRITICAL PATH - Eager Initialization ==========

        val coreMetaData = CoreMetaData()
        val validator = Validator()
        val validationResultStack = ValidationResultStack()
        val ctLockManager = CTLockManager()
        val mainLooperHandler = MainLooperHandler()
        val executors = CTExecutorFactory.executors(config)

        // Cryptography
        val repository = CryptRepository(context, accountId)
        val ctKeyGenerator = CTKeyGenerator(repository)
        val cryptFactory = CryptFactory(accountId, ctKeyGenerator)
        val cryptHandler = CryptHandler(repository, cryptFactory)

        // Device info
        val deviceInfo = DeviceInfo(context, config, cleverTapID, coreMetaData)

        // Core feature
        val coreFeature = CoreFeature(
            context = context,
            config = config,
            deviceInfo = deviceInfo,
            coreMetaData = coreMetaData,
            executors = executors,
            mainLooperHandler = mainLooperHandler,
            validationResultStack = validationResultStack,
            cryptHandler = cryptHandler,
            ctLockManager = ctLockManager
        )

        // Network layer
        val networkRepo = NetworkRepo(context, config)
        val networkFeature = NetworkFeature(networkRepo, ctKeyGenerator, cryptFactory)

        // Database
        val storeProvider = StoreProvider.getInstance()
        val dbEncryptionHandler = DBEncryptionHandler(
            crypt = cryptHandler,
            logger = config.logger,
            encryptionLevel = EncryptionLevel.fromInt(config.encryptionLevel)
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
        val dataFeature = DataFeature(databaseManager, localDataStore)

        // ========== FEATURE PROVIDERS - Lazy (NOT executed) ==========

        config.logger.info(accountId, "Creating feature providers (lazy)...")

        // Analytics Provider
        val analyticsProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating AnalyticsFeature")
            val profileValueHandler = ProfileValueHandler(validator, validationResultStack)
            CTPreferenceCache.getInstance(context, config)
            val loginInfoProvider = LoginInfoProvider(context, config, cryptHandler)
            AnalyticsFeature(
                networkFeature = networkFeature,
                validator = validator,
                profileValueHandler = profileValueHandler,
                loginInfoProvider = loginInfoProvider
            )
        }

        // Profile Provider
        val profileProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating ProfileFeature")
            val profileValueHandler = ProfileValueHandler(validator, validationResultStack)
            val loginInfoProvider = LoginInfoProvider(context, config, cryptHandler)
            ProfileFeature(
                loginInfoProvider = loginInfoProvider,
                profileValueHandler = profileValueHandler
            )
        }

        // InApp Provider
        val inAppProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating InAppFeature")
            val storeRegistry = StoreRegistry(
                inAppStore = null,
                impressionStore = null,
                legacyInAppStore = storeProvider.provideLegacyInAppStore(context, accountId),
                inAppAssetsStore = storeProvider.provideInAppAssetsStore(context, accountId),
                filesStore = storeProvider.provideFileStore(context, accountId)
            )
            InAppFeature(
                dataFeature = dataFeature,
                storeRegistry = storeRegistry,
                storeProvider = storeProvider
            )
        }

        // Inbox Provider
        val inboxProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating InboxFeature")
            InboxFeature()
        }

        // Variables Provider
        val variablesProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating VariablesFeature")
            val storeRegistry = StoreRegistry(
                inAppStore = null,
                impressionStore = null,
                legacyInAppStore = storeProvider.provideLegacyInAppStore(context, accountId),
                inAppAssetsStore = storeProvider.provideInAppAssetsStore(context, accountId),
                filesStore = storeProvider.provideFileStore(context, accountId)
            )
            VariablesFeature(
                storeRegistry = storeRegistry,
                dbEncryptionHandler = dbEncryptionHandler
            )
        }

        // Push Provider
        val pushProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating PushFeature")
            PushFeature()
        }

        // ProductConfig Provider
        val productConfigProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating ProductConfigFeature")
            ProductConfigFeature()
        }

        // DisplayUnit Provider
        val displayUnitProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating DisplayUnitFeature")
            DisplayUnitFeature()
        }

        // FeatureFlag Provider
        val featureFlagProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating FeatureFlagFeature")
            FeatureFlagFeature()
        }

        // Geofence Provider
        val geofenceProvider = {
            config.logger.verbose(accountId, "[PROVIDER] Creating GeofenceFeature")
            GeofenceFeature()
        }

        val state = CoreState(
            core = coreFeature,
            data = dataFeature,
            network = networkFeature,
            analyticsProvider = analyticsProvider,
            profileProvider = profileProvider,
            inAppProvider = inAppProvider,
            inboxProvider = inboxProvider,
            variablesProvider = variablesProvider,
            pushProvider = pushProvider,
            productConfigProvider = productConfigProvider,
            displayUnitProvider = displayUnitProvider,
            featureFlagProvider = featureFlagProvider,
            geofenceProvider = geofenceProvider
        )

        config.logger.info(accountId, "CoreState created - features will initialize on-demand")

        state.asyncStartup()

        return state
    }
}