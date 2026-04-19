package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.FetchInboxCallback
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.NetworkScope
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class InboxV2BridgeTest {

    @Test
    fun `submit launches fetcher and invokes callback with true on Success`() = runTest {
        val fetcher = mockk<InboxV2Fetcher> {
            coEvery { fetch(any()) } returns CallResult.Success(Unit)
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val bridge = InboxV2Bridge(fetcher, scope)

        var received: Boolean? = null
        bridge.submit(respectThrottle = true, callback = FetchInboxCallback { received = it })
        advanceUntilIdle()

        assertEquals(true, received)
    }

    @Test
    fun `callback is false on non-Success results`() = runTest {
        val fetcher = mockk<InboxV2Fetcher> {
            coEvery { fetch(any()) } returns CallResult.Throttled
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val bridge = InboxV2Bridge(fetcher, scope)

        var received: Boolean? = null
        bridge.submit(respectThrottle = true, callback = FetchInboxCallback { received = it })
        advanceUntilIdle()

        assertEquals(false, received)
    }

    @Test
    fun `null callback does not crash`() = runTest {
        val fetcher = mockk<InboxV2Fetcher> {
            coEvery { fetch(any()) } returns CallResult.Success(Unit)
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val bridge = InboxV2Bridge(fetcher, scope)

        bridge.submit(respectThrottle = false, callback = null)
        advanceUntilIdle()
    }

    @Test
    fun `cancelling the scope stops the pending launch`() = runTest {
        val fetcher = mockk<InboxV2Fetcher> {
            coEvery { fetch(any()) } coAnswers {
                delay(1_000)
                CallResult.Success(Unit)
            }
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val bridge = InboxV2Bridge(fetcher, scope)

        var received: Boolean? = null
        bridge.submit(respectThrottle = false, callback = FetchInboxCallback { received = it })
        scope.cancel()
        advanceUntilIdle()

        assertNull(received)
    }
}
