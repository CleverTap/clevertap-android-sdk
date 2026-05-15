package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.EndpointCall
import com.clevertap.android.sdk.network.fetch.FetchThrottle
import com.clevertap.android.sdk.network.fetch.FetchTrigger
import com.clevertap.android.sdk.response.InboxV2Response
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InboxV2FetcherTest {

    private fun stubEndpoint(
        result: CallResult<JSONObject> = CallResult.Success(JSONObject())
    ): EndpointCall<JSONObject> = mockk(relaxed = true) {
        coEvery { execute() } returns result
    }

    private fun nonThrottling(): FetchThrottle = mockk(relaxed = true) {
        every { shouldThrottle() } returns false
    }

    private fun noopResponse(): InboxV2Response = mockk(relaxed = true) {
        every { processResponse(any()) } just Runs
    }

    private fun noopLogger(): Logger = mockk(relaxed = true)

    @Test
    fun `USER_INITIATED returns Throttled when throttle is hot`() = runTest {
        val endpoint = stubEndpoint()
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns true
        }

        val result = InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.USER_INITIATED)

        assertEquals(CallResult.Throttled, result)
        coVerify(exactly = 0) { endpoint.execute() }
        verify(exactly = 0) { throttle.recordFetch() }
    }

    @Test
    fun `SYSTEM bypasses throttle and does NOT record`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns true
        }
        val endpoint = stubEndpoint(CallResult.Success(JSONObject()))

        val result = InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        assertTrue(result is CallResult.Success)
        verify(exactly = 0) { throttle.recordFetch() }
    }

    @Test
    fun `Disabled from endpoint flips session flag and short-circuits the next call`() = runTest {
        val endpoint = stubEndpoint(CallResult.Disabled)
        val fetcher = InboxV2Fetcher(endpoint, nonThrottling(), noopResponse(), noopLogger())

        val first = fetcher.fetch(FetchTrigger.SYSTEM)
        val second = fetcher.fetch(FetchTrigger.SYSTEM)

        assertEquals(CallResult.Disabled, first)
        assertEquals(CallResult.Disabled, second)
        coVerify(exactly = 1) { endpoint.execute() }
    }

    @Test
    fun `Success delegates JSON to InboxV2Response processResponse`() = runTest {
        val response = mockk<InboxV2Response>(relaxed = true)
        val captured = slot<JSONObject>()
        every { response.processResponse(capture(captured)) } just Runs

        val json = JSONObject().put("inbox_notifs_v2", "x")
        val endpoint = stubEndpoint(CallResult.Success(json))

        val result = InboxV2Fetcher(endpoint, nonThrottling(), response, noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        assertTrue(result is CallResult.Success)
        assertSame(json, captured.captured)
    }

    @Test
    fun `HttpError propagates through`() = runTest {
        val endpoint = stubEndpoint(CallResult.HttpError(500, "oops"))

        val result = InboxV2Fetcher(endpoint, nonThrottling(), noopResponse(), noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        assertEquals(CallResult.HttpError(500, "oops"), result)
    }

    @Test
    fun `NetworkFailure propagates through`() = runTest {
        val cause = IOException("bad")
        val endpoint = stubEndpoint(CallResult.NetworkFailure(cause))

        val result = InboxV2Fetcher(endpoint, nonThrottling(), noopResponse(), noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        assertTrue(result is CallResult.NetworkFailure)
        assertSame(cause, (result as CallResult.NetworkFailure).cause)
    }

    @Test
    fun `USER_INITIATED records throttle on Success`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns false
        }
        val endpoint = stubEndpoint(CallResult.Success(JSONObject()))

        InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.USER_INITIATED)

        verify(exactly = 1) { throttle.recordFetch() }
    }

    @Test
    fun `USER_INITIATED records throttle on HttpError`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns false
        }
        val endpoint = stubEndpoint(CallResult.HttpError(500, "err"))

        InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.USER_INITIATED)

        verify(exactly = 1) { throttle.recordFetch() }
    }

    @Test
    fun `USER_INITIATED does NOT record throttle on NetworkFailure — retry is not blocked`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns false
        }
        val endpoint = stubEndpoint(CallResult.NetworkFailure(IOException("timeout")))

        InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.USER_INITIATED)

        verify(exactly = 0) { throttle.recordFetch() }
    }

    @Test
    fun `SYSTEM does NOT record throttle on Success`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns false
        }
        val endpoint = stubEndpoint(CallResult.Success(JSONObject()))

        InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        verify(exactly = 0) { throttle.recordFetch() }
    }

    @Test
    fun `SYSTEM does NOT record throttle on HttpError`() = runTest {
        val throttle = mockk<FetchThrottle>(relaxed = true) {
            every { shouldThrottle() } returns false
        }
        val endpoint = stubEndpoint(CallResult.HttpError(503, "unavailable"))

        InboxV2Fetcher(endpoint, throttle, noopResponse(), noopLogger())
            .fetch(FetchTrigger.SYSTEM)

        verify(exactly = 0) { throttle.recordFetch() }
    }
}
