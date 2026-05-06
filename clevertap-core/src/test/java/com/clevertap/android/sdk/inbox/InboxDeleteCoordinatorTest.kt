package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.dao.PendingDelete
import com.clevertap.android.sdk.network.QueueHeaderBuilder
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.fetch.NetworkScope
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class InboxDeleteCoordinatorTest {

    private class StubHttpClient(val code: Int) : CtHttpClient {
        override fun execute(request: Request): Response = Response(
            request = request,
            code = code,
            headers = emptyMap(),
            bodyStream = "".byteInputStream()
        ) {}
    }

    private class CapturingHttpClient(
        var responseCode: Int = 200,
        var responseBody: String? = ""
    ) : CtHttpClient {
        var lastRequest: Request? = null
        override fun execute(request: Request): Response {
            lastRequest = request
            return Response(
                request = request,
                code = responseCode,
                headers = emptyMap(),
                bodyStream = responseBody?.byteInputStream()
            ) {}
        }
    }

    private fun ctApi(httpClient: CtHttpClient): CtApi = CtApi(
        httpClient = httpClient,
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

    private fun ctApi(code: Int): CtApi = ctApi(StubHttpClient(code))

    private fun headerBuilder(): QueueHeaderBuilder = mockk<QueueHeaderBuilder>(relaxed = true).apply {
        every { buildHeader(null) } returns JSONObject().put("g", "guid-xyz")
    }

    private fun message(id: String): CTInboxMessage =
        CTInboxMessage(JSONObject().put("id", id))

    private fun TestScope.coordinator(
        code: Int,
        dbAdapter: DBAdapter
    ): InboxDeleteCoordinator = InboxDeleteCoordinator(
        networkScope = NetworkScope(StandardTestDispatcher(testScheduler)),
        ctApi = ctApi(code),
        queueHeaderBuilder = headerBuilder(),
        dbAdapterProvider = { dbAdapter },
        coreMetaData = mockk<CoreMetaData>(relaxed = true),
        packageName = "com.example.app",
        logger = mockk<Logger>(relaxed = true),
        httpDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    private fun TestScope.capturingCoordinator(
        http: CapturingHttpClient,
        dbAdapter: DBAdapter
    ): InboxDeleteCoordinator = InboxDeleteCoordinator(
        networkScope = NetworkScope(StandardTestDispatcher(testScheduler)),
        ctApi = ctApi(http),
        queueHeaderBuilder = headerBuilder(),
        dbAdapterProvider = { dbAdapter },
        coreMetaData = mockk<CoreMetaData>(relaxed = true),
        packageName = "com.example.app",
        logger = mockk<Logger>(relaxed = true),
        httpDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    @Test
    fun `syncDelete with empty list is a no-op and does not clear pending rows`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val coordinator = coordinator(200, dbAdapter)

        coordinator.syncDelete(emptyList(), "u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }

    @Test
    fun `syncDelete on 200 batch-removes every pending row for the ids sent`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val coordinator = coordinator(200, dbAdapter)

        coordinator.syncDelete(listOf(message("m1"), message("m2"), message("m3")), "u")
        advanceUntilIdle()

        verify(exactly = 1) { dbAdapter.removePendingDeletes(listOf("m1", "m2", "m3"), "u") }
    }

    @Test
    fun `syncDelete on non-2xx leaves every pending row for the next retry`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val coordinator = coordinator(500, dbAdapter)

        coordinator.syncDelete(listOf(message("m1"), message("m2")), "u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }

    @Test
    fun `retryPending drains every pending row for the user in one call`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true).apply {
            every { getPendingDeletes("u") } returns listOf(
                PendingDelete("m1", null),
                PendingDelete("m2", null)
            )
        }
        val coordinator = coordinator(200, dbAdapter)

        coordinator.retryPending("u")
        advanceUntilIdle()

        verify { dbAdapter.removePendingDeletes(match { it.toSet() == setOf("m1", "m2") }, "u") }
    }

    @Test
    fun `retryPending replays original wzrkParams on each message`() = runTest {
        val capturing = CapturingHttpClient(responseCode = 200)
        val dbAdapter = mockk<DBAdapter>(relaxed = true).apply {
            every { getPendingDeletes("u") } returns listOf(
                PendingDelete("m1", JSONObject().put("wzrk_id", "c1").put("wzrk_pivot", "default")),
                PendingDelete("m2", JSONObject().put("wzrk_id", "c2"))
            )
        }
        val coordinator = capturingCoordinator(capturing, dbAdapter)

        coordinator.retryPending("u")
        advanceUntilIdle()

        val payload = org.json.JSONArray(capturing.lastRequest!!.body).getJSONObject(1)
        val msgs = payload.getJSONArray("messages")
        assertEquals(2, msgs.length())
        assertEquals("m1", msgs.getJSONObject(0).getString("wzrk_mid"))
        assertEquals("c1", msgs.getJSONObject(0).getString("wzrk_id"))
        assertEquals("default", msgs.getJSONObject(0).getString("wzrk_pivot"))
        assertEquals("m2", msgs.getJSONObject(1).getString("wzrk_mid"))
        assertEquals("c2", msgs.getJSONObject(1).getString("wzrk_id"))
    }

    @Test
    fun `retryPending with no pending rows is a no-op`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true).apply {
            every { getPendingDeletes("u") } returns emptyList()
        }
        val coordinator = coordinator(200, dbAdapter)

        coordinator.retryPending("u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }
}
