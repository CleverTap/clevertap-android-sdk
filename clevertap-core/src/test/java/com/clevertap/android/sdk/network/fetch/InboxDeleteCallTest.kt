package com.clevertap.android.sdk.network.fetch

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
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
class InboxDeleteCallTest {

    private class CapturingHttpClient(
        var responseCode: Int = 200,
        var responseBody: String? = "",
        var throwOnExecute: Boolean = false
    ) : CtHttpClient {
        var lastRequest: Request? = null

        override fun execute(request: Request): Response {
            lastRequest = request
            if (throwOnExecute) throw RuntimeException("boom")
            return Response(
                request = request,
                code = responseCode,
                headers = emptyMap(),
                bodyStream = responseBody?.byteInputStream(Charsets.UTF_8)
            ) {}
        }
    }

    private fun newCtApi(http: CtHttpClient): CtApi =
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

    private fun inboxMessage(id: String, wzrk: Map<String, String> = emptyMap()): CTInboxMessage {
        val json = JSONObject().put("id", id)
        if (wzrk.isNotEmpty()) {
            val params = JSONObject()
            wzrk.forEach { (k, v) -> params.put(k, v) }
            json.put("wzrkParams", params)
        }
        return CTInboxMessage(json)
    }

    private fun newCall(
        http: CtHttpClient,
        messages: List<CTInboxMessage>,
        header: JSONObject? = JSONObject().put("g", "guid-xyz")
    ): InboxDeleteCall = InboxDeleteCall(
        ctApi = newCtApi(http),
        queueHeaderBuilder = newHeaderBuilder(header),
        messages = messages,
        logger = mockk<Logger>(relaxed = true),
        dispatcher = UnconfinedTestDispatcher()
    )

    @Test
    fun `empty list short-circuits to Success without hitting the network`() = runTest {
        val http = CapturingHttpClient()

        val result = newCall(http, emptyList()).execute()

        assertEquals(CallResult.Success(Unit), result)
        assertEquals(null, http.lastRequest)
    }

    @Test
    fun `HTTP 200 returns Success Unit`() = runTest {
        val result = newCall(CapturingHttpClient(responseCode = 200), listOf(inboxMessage("m1"))).execute()

        assertEquals(CallResult.Success(Unit), result)
    }

    @Test
    fun `HTTP 403 returns Disabled`() = runTest {
        val result = newCall(CapturingHttpClient(responseCode = 403), listOf(inboxMessage("m1"))).execute()

        assertEquals(CallResult.Disabled, result)
    }

    @Test
    fun `HTTP 500 returns HttpError with body`() = runTest {
        val result = newCall(
            CapturingHttpClient(responseCode = 500, responseBody = "oh no"),
            listOf(inboxMessage("m1"))
        ).execute()

        assertEquals(CallResult.HttpError(500, "oh no"), result)
    }

    @Test
    fun `exception on send returns NetworkFailure`() = runTest {
        val result = newCall(
            CapturingHttpClient(throwOnExecute = true),
            listOf(inboxMessage("m1"))
        ).execute()

        assertTrue(result is CallResult.NetworkFailure)
    }

    @Test
    fun `batch body carries one entry per message with wzrk params inlined`() = runTest {
        val http = CapturingHttpClient(responseCode = 200)
        val msgs = listOf(
            inboxMessage("m1", mapOf("wzrk_id" to "camp-1", "wzrk_pivot" to "default")),
            inboxMessage("m2", mapOf("wzrk_id" to "camp-2"))
        )

        newCall(http, msgs).execute()

        val bodyJson = JSONArray(requireNotNull(http.lastRequest?.body))
        val messagesArray = bodyJson.getJSONObject(1)
            .getJSONObject("evtData")
            .getJSONArray("messages")
        assertEquals(2, messagesArray.length())
        assertEquals("m1", messagesArray.getJSONObject(0).getString("_id"))
        assertEquals("camp-1", messagesArray.getJSONObject(0).getString("wzrk_id"))
        assertEquals("default", messagesArray.getJSONObject(0).getString("wzrk_pivot"))
        assertEquals("m2", messagesArray.getJSONObject(1).getString("_id"))
        assertEquals("camp-2", messagesArray.getJSONObject(1).getString("wzrk_id"))
    }

    @Test
    fun `null header from builder returns NetworkFailure without hitting network`() = runTest {
        val http = CapturingHttpClient(responseCode = 200, responseBody = "should-not-be-read")

        val result = newCall(http, listOf(inboxMessage("m1")), header = null).execute()

        assertTrue(result is CallResult.NetworkFailure)
        assertEquals(null, http.lastRequest)
    }
}
