package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.InboxV2Bridge
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk

internal class MockCoreStateKotlin(cleverTapInstanceConfig: CleverTapInstanceConfig) : CoreState(
    mockk(relaxed = true),
    cleverTapInstanceConfig,
    CoreMetaData(),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    CTLockManager(),
    CallbackManager(cleverTapInstanceConfig, mockk(relaxed = true)),
    mockk(relaxed = true), // controllerManager
    mockk(relaxed = true), // inAppController
    mockk(relaxed = true), // evaluationManager
    mockk(relaxed = true), // impressionManager
    mockk(relaxed = true), // ndImpressionManager
    mockk(relaxed = true), // ndTriggerManager
    mockk(relaxed = true), // loginController
    mockk(relaxed = true), // sessionManager
    ValidationResultStack(),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    MockCTExecutors(),
    mockk<InboxV2Bridge>(relaxed = true)
)
