package com.clevertap.android.sdk

import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk

internal class MockCoreStateKotlin(cleverTapInstanceConfig: CleverTapInstanceConfig) : CoreState(
    context = mockk(relaxed = true),
    locationManager = mockk(relaxed = true),
    config = cleverTapInstanceConfig,
    coreMetaData = CoreMetaData(),
    databaseManager = mockk(relaxed = true),
    deviceInfo = mockk(relaxed = true),
    eventMediator = mockk(relaxed = true),
    localDataStore = mockk(relaxed = true),
    activityLifeCycleManager = mockk(relaxed = true),
    analyticsManager = mockk(relaxed = true),
    baseEventQueueManager = mockk(relaxed = true),
    cTLockManager = CTLockManager(),
    callbackManager = CallbackManager(cleverTapInstanceConfig, mockk(relaxed = true)),
    controllerManager = mockk(relaxed = true),
    inAppController = mockk(relaxed = true),
    evaluationManager = mockk(relaxed = true),
    impressionManager = mockk(relaxed = true),
    sessionManager = mockk(relaxed = true),
    validationResultStack = ValidationResultStack(),
    mainLooperHandler = mockk(relaxed = true),
    networkManager = mockk(relaxed = true),
    pushProviders = mockk(relaxed = true),
    varCache = mockk(relaxed = true),
    parser = mockk(relaxed = true),
    cryptHandler = mockk(relaxed = true),
    storeRegistry = mockk(relaxed = true),
    templatesManager = mockk(relaxed = true),
    profileValueHandler = mockk(relaxed = true),
    cTVariables = mockk(relaxed = true),
    executors = MockCTExecutors(),
    contentFetchManager = mockk(relaxed = true),
    loginInfoProvider = mockk(relaxed = true)
)
