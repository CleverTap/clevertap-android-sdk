package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.CleverTapInstanceConfig
import io.mockk.every
import io.mockk.mockk

fun configMock(): CleverTapInstanceConfig {
    val mockConfig = mockk<CleverTapInstanceConfig>()

    every { mockConfig.logger } returns mockk(relaxed = true)
    every { mockConfig.accountId } returns "Test-Account-ID"

    return mockConfig
}
