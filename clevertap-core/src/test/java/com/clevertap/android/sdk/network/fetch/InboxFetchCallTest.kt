package com.clevertap.android.sdk.network.fetch

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.http.MockHttpClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class InboxFetchCallTest {

    private fun newCtApi(http: MockHttpClient): CtApi =
        CtApi(
            httpClient = http,
            defaultDomain = "domain.com",
            cachedDomain = null,
            cachedSpikyDomain = null,
            region = "region",
            proxyDomain = null,
            spikyProxyDomain = null,
            customHandshakeDomain = null,
            accountId = "acct",
            accountToken = "token",
            sdkVersion = "x.x.x-test",
            logger = mockk(relaxed = true),
            logTag = "testCtApi"
        )

    private fun newHeaderBuilder(header: JSONObject? = JSONObject().put("g", "guid-xyz")): QueueHeaderBuilder {
        val builder = mockk<QueueHeaderBuilder>(relaxed = true)
        every { builder.buildHeader(null) } returns header
        return builder
    }

    private fun noopLogger(): Logger = mockk(relaxed = true)

    private fun newCall(http: MockHttpClient, header: JSONObject? = JSONObject().put("g", "guid-xyz")): InboxFetchCall =
        InboxFetchCall(
            ctApi = newCtApi(http),
            queueHeaderBuilder = newHeaderBuilder(header),
            logger = noopLogger(),
            dispatcher = UnconfinedTestDispatcher()
        )

    @Test
    fun `HTTP 200 with parseable body returns Success`() = runTest {
        val http = MockHttpClient(responseCode = 200, responseBody = """{"inbox_notifs_v2":[]}""")

        val result = newCall(http).execute()

        assertTrue(result is CallResult.Success)
        assertEquals(0, (result as CallResult.Success).data.getJSONArray("inbox_notifs_v2").length())
    }

    @Test
    fun `HTTP 403 returns Disabled`() = runTest {
        val http = MockHttpClient(responseCode = 403, responseBody = """{"code":10006}""")

        val result = newCall(http).execute()

        assertEquals(CallResult.Disabled, result)
    }

    @Test
    fun `HTTP 500 returns HttpError with body`() = runTest {
        val http = MockHttpClient(responseCode = 500, responseBody = "oh no")

        val result = newCall(http).execute()

        assertEquals(CallResult.HttpError(500, "oh no"), result)
    }

    @Test
    fun `HTTP 200 with null body returns NetworkFailure`() = runTest {
        val http = MockHttpClient(responseCode = 200, responseBody = null)

        val result = newCall(http).execute()

        assertTrue(result is CallResult.NetworkFailure)
    }

    @Test
    fun `exception during send returns NetworkFailure`() = runTest {
        val http = MockHttpClient().apply { alwaysThrowOnExecute = true }

        val result = newCall(http).execute()

        assertTrue(result is CallResult.NetworkFailure)
    }

    @Test
    fun `malformed JSON body returns NetworkFailure`() = runTest {
        val http = MockHttpClient(responseCode = 200, responseBody = "not-json")

        val result = newCall(http).execute()

        assertTrue(result is CallResult.NetworkFailure)
    }

    @Test
    fun `null header from builder returns NetworkFailure without hitting network`() = runTest {
        val http = MockHttpClient(responseCode = 200, responseBody = "should-not-be-read")

        val result = newCall(http, header = null).execute()

        assertTrue(result is CallResult.NetworkFailure)
    }
}
