package com.clevertap

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreState
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
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.Constant
import io.mockk.mockk

internal object CleverTapPlug {

    fun provideClevertapInstanceConfig(
        context: Context = mockk<Context>(),
        block: CleverTapInstanceConfig.() -> Unit = {}
    ): CleverTapInstanceConfig {
        return CleverTapInstanceConfig.createInstance(
            context,
            Constant.ACC_ID,
            Constant.ACC_TOKEN
        ).apply(block)
    }

    /**
     * Builds a CoreState instance with mock providers, allowing for selective overrides.
     * This is useful when you need a CoreState with some real and some mocked dependencies.
     */
    fun buildCoreState(
        coreFeature: CoreFeature = mockk(relaxed = true),
        dataFeature: DataFeature = mockk(relaxed = true),
        networkFeature: NetworkFeature = mockk(relaxed = true),
        analyticsProvider: () -> AnalyticsFeature = { mockk(relaxed = true) },
        profileProvider: () -> ProfileFeature = { mockk(relaxed = true) },
        inAppProvider: () -> InAppFeature = { mockk(relaxed = true) },
        inboxProvider: () -> InboxFeature = { mockk(relaxed = true) },
        variablesProvider: () -> VariablesFeature = { mockk(relaxed = true) },
        pushProvider: () -> PushFeature = { mockk(relaxed = true) },
        productConfigProvider: () -> ProductConfigFeature = { mockk(relaxed = true) },
        displayUnitProvider: () -> DisplayUnitFeature = { mockk(relaxed = true) },
        featureFlagProvider: () -> FeatureFlagFeature = { mockk(relaxed = true) },
        geofenceProvider: () -> GeofenceFeature = { mockk(relaxed = true) }
    ): CoreState {
        return CoreState(
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
    }

    /**
     * Builds a CoreState instance where every direct and lazily-initialized property is a relaxed mock.
     * This is ideal for unit tests where CoreState is a dependency and you want to avoid side effects.
     */
    fun mockCoreState(): CoreState {
        return CoreState(
            core = mockk(relaxed = true),
            data = mockk(relaxed = true),
            network = mockk(relaxed = true),
            analyticsProvider = { mockk(relaxed = true) },
            profileProvider = { mockk(relaxed = true) },
            inAppProvider = { mockk(relaxed = true) },
            inboxProvider = { mockk(relaxed = true) },
            variablesProvider = { mockk(relaxed = true) },
            pushProvider = { mockk(relaxed = true) },
            productConfigProvider = { mockk(relaxed = true) },
            displayUnitProvider = { mockk(relaxed = true) },
            featureFlagProvider = { mockk(relaxed = true) },
            geofenceProvider = { mockk(relaxed = true) }
        )
    }

    /**
     * Builds a CoreState instance where each feature class is instantiated with fully mocked dependencies.
     * This is useful for tests that need to interact with the real feature class logic but want to isolate
     * it from its own dependencies (like database, network, or config layers).
     */
    fun deeplyMockedCoreState(
        context: Context = mockk(relaxed = true),
        clock: Clock = mockk(relaxed = true),
        executors: CTExecutors = mockk(relaxed = true),
        config: CleverTapInstanceConfig = provideClevertapInstanceConfig(context)
    ): CoreState {

        // Now, instantiate the real feature classes using the mocked dependencies.
        val coreFeature = CoreFeature(
            context = mockk(relaxed = true),
            config = config,
            deviceInfo = mockk(relaxed = true),
            coreMetaData = mockk(relaxed = true),
            executors = executors,
            mainLooperHandler = mockk(relaxed = true),
            validationResultStack = mockk(relaxed = true),
            cryptHandler = mockk(relaxed = true),
            ctLockManager = mockk(relaxed = true),
            clock = clock,
            arpRepo = mockk(relaxed = true)
        )

        val dataFeature = DataFeature(
            databaseManager = mockk(relaxed = true),
            localDataStore = mockk(relaxed = true),
        )
        val networkFeature = NetworkFeature(
            networkRepo = mockk(relaxed = true),
            ctKeyGenerator = mockk(relaxed = true),
            cryptFactory = mockk(relaxed = true),
        )

        // Return a new CoreState instance built from these real features with mocked dependencies.
        // The lazy providers will also return real features with mocked dependencies.
        val coreState = buildCoreState(
            coreFeature = coreFeature,
            dataFeature = dataFeature,
            networkFeature = networkFeature,
            analyticsProvider = {
                AnalyticsFeature(
                    networkFeature = mockk(relaxed = true),
                    validator = mockk(relaxed = true),
                    profileValueHandler = mockk(relaxed = true),
                    loginInfoProvider = mockk(relaxed = true),
                )
            },
            profileProvider = {
                ProfileFeature(
                    loginInfoProvider = mockk(relaxed = true),
                    profileValueHandler = mockk(relaxed = true)
                )
            },
            inAppProvider = {
                InAppFeature(
                    dataFeature = mockk(relaxed = true),
                    storeRegistry = mockk(relaxed = true),
                    storeProvider = mockk(relaxed = true),
                )
            },
            inboxProvider = {
                InboxFeature(
                    mainPost = { it.invoke() } // just run in sync since its a test class
                )
            },
            variablesProvider = {
                VariablesFeature(
                    storeRegistry = mockk(relaxed = true),
                    dbEncryptionHandler = mockk(relaxed = true),
                    fetchVariablesCallback = mockk(relaxed = true)
                )
            },
            pushProvider = {
                PushFeature()
            },
            productConfigProvider = {
                ProductConfigFeature(
                    callbacks = mockk(relaxed = true)
                )
            },
            displayUnitProvider = {
                DisplayUnitFeature() // set in specific test case.
            },
            featureFlagProvider = {
                FeatureFlagFeature()
            },
            geofenceProvider = {
                GeofenceFeature()
            }
        )

        return coreState
    }
}