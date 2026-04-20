package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.InboxDeleteCoordinator
import com.clevertap.android.sdk.inbox.InboxV2Bridge
import com.clevertap.android.sdk.network.fetch.NetworkScope
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
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
    mockk(relaxed = true),
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
    NetworkScope(),
    mockk<InboxV2Bridge>(relaxed = true),
    mockk<InboxDeleteCoordinator>(relaxed = true)
)
