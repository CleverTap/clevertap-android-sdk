package com.clevertap.android.sdk.utils

import android.os.Handler
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.task.MainLooperHandler
import io.mockk.every
import io.mockk.mockk

internal fun handlerMock(): Handler {
    return configHandlerMock(mockk())
}

internal fun mainHandlerMock(): MainLooperHandler {
    return configHandlerMock(mockk<MainLooperHandler>())
}

private fun <T : Handler> configHandlerMock(mock: T): T {
    every { mock.post(any()) } answers {
        (args[0] as Runnable).run()
        true
    }

    every { mock.postDelayed(any(), any()) } answers {
        (args[0] as Runnable).run()
        true
    }

    return mock
}

fun configMock(): CleverTapInstanceConfig {
    val mockConfig = mockk<CleverTapInstanceConfig>()

    every { mockConfig.logger } returns mockk(relaxed = true)
    every { mockConfig.accountId } returns "Test-Account-ID"

    return mockConfig
}
