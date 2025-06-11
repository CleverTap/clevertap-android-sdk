package com.clevertap.android.sdk

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
    mockk(relaxed = true)
)
