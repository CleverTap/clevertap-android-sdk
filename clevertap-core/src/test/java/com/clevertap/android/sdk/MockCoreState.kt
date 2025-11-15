package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.features.*
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

internal class MockCoreStateKotlin(
    cleverTapInstanceConfig: CleverTapInstanceConfig, 
    context: Context? = null
) : CoreState(
    core = createMockCoreFeature(cleverTapInstanceConfig, context),
    data = createMockDataFeature(),
    network = createMockNetworkFeature(),
    analytics = createMockAnalyticsFeature(),
    profileFeat = createMockProfileFeature(),
    inApp = createMockInAppFeature(),
    inbox = createMockInboxFeature(),
    variables = createMockVariablesFeature(),
    push = createMockPushFeature(),
    lifecycle = createMockLifecycleFeature(),
    callback = createMockCallbackFeature(cleverTapInstanceConfig)
) {
    init {
        asyncStartup()
    }
}

private fun createMockCoreFeature(
    config: CleverTapInstanceConfig,
    context: Context?
): CoreFeature {
    return mockk(relaxed = true) {
        every { this@mockk.context } returns (context ?: mockk(relaxed = true))
        every { this@mockk.config } returns config
        every { this@mockk.coreMetaData } returns CoreMetaData()
        every { this@mockk.deviceInfo } returns mockk(relaxed = true)
        every { this@mockk.validationResultStack } returns ValidationResultStack()
        every { this@mockk.mainLooperHandler } returns mockk(relaxed = true)
        every { this@mockk.executors } returns MockCTExecutors()
        every { this@mockk.cryptHandler } returns mockk(relaxed = true)
        every { this@mockk.clock } returns mockk(relaxed = true)
    }
}

private fun createMockDataFeature(): DataFeature {
    return mockk(relaxed = true) {
        every { this@mockk.databaseManager } returns mockk(relaxed = true)
        every { this@mockk.localDataStore } returns mockk(relaxed = true)
        every { this@mockk.storeRegistry } returns mockk(relaxed = true)
        every { this@mockk.storeProvider } returns mockk(relaxed = true)
    }
}

private fun createMockNetworkFeature(): NetworkFeature {
    return mockk(relaxed = true) {
        every { this@mockk.networkManager } returns mockk(relaxed = true)
        every { this@mockk.contentFetchManager } returns mockk(relaxed = true)
    }
}

private fun createMockAnalyticsFeature(): AnalyticsFeature {
    return mockk(relaxed = true) {
        every { this@mockk.analyticsManager } returns mockk(relaxed = true)
        every { this@mockk.baseEventQueueManager } returns mockk(relaxed = true)
        every { this@mockk.sessionManager } returns mockk(relaxed = true)
        every { this@mockk.eventMediator } returns mockk(relaxed = true)
    }
}

private fun createMockProfileFeature(): ProfileFeature {
    return mockk(relaxed = true) {
        every { this@mockk.loginInfoProvider } returns mockk(relaxed = true)
        every { this@mockk.profileValueHandler } returns mockk(relaxed = true)
        every { this@mockk.locationManager } returns mockk(relaxed = true)
    }
}

private fun createMockInAppFeature(): InAppFeature {
    return mockk(relaxed = true) {
        every { this@mockk.inAppController } returns mockk(relaxed = true)
        every { this@mockk.evaluationManager } returns mockk(relaxed = true)
        every { this@mockk.impressionManager } returns mockk(relaxed = true)
        every { this@mockk.templatesManager } returns mockk(relaxed = true)
        every { this@mockk.inAppFCManager } returns null
    }
}

private fun createMockInboxFeature(): InboxFeature {
    return mockk(relaxed = true) {
        every { this@mockk.cTLockManager } returns CTLockManager()
        every { this@mockk.ctInboxController } returns null
    }
}

private fun createMockVariablesFeature(): VariablesFeature {
    return mockk(relaxed = true) {
        every { this@mockk.cTVariables } returns mockk(relaxed = true)
        every { this@mockk.varCache } returns mockk(relaxed = true)
        every { this@mockk.parser } returns mockk(relaxed = true)
        every { this@mockk.variablesRepository } returns mockk(relaxed = true)
    }
}

private fun createMockPushFeature(): PushFeature {
    return mockk(relaxed = true) {
        every { this@mockk.pushProviders } returns mockk(relaxed = true)
    }
}

private fun createMockLifecycleFeature(): LifecycleFeature {
    return mockk(relaxed = true) {
        every { this@mockk.activityLifeCycleManager } returns mockk(relaxed = true)
    }
}

private fun createMockCallbackFeature(config: CleverTapInstanceConfig): CallbackFeature {
    return mockk(relaxed = true) {
        every { this@mockk.callbackManager } returns spyk(
            CallbackManager(config, mockk(relaxed = true))
        )
    }
}
