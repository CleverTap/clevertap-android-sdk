package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DBAdapter
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    private fun ctApi(code: Int): CtApi = CtApi(
        httpClient = StubHttpClient(code),
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

    private fun headerBuilder(): QueueHeaderBuilder = mockk<QueueHeaderBuilder>(relaxed = true).apply {
        every { buildHeader(null) } returns JSONObject().put("g", "guid-xyz")
    }

    private fun message(id: String): CTInboxMessage =
        CTInboxMessage(JSONObject().put("id", id))

    @Test
    fun `syncDelete with empty list is a no-op and does not clear pending rows`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val coordinator = InboxDeleteCoordinator(
            scope, ctApi(200), headerBuilder(), { dbAdapter }, mockk<Logger>(relaxed = true),
            UnconfinedTestDispatcher(testScheduler)
        )

        coordinator.syncDelete(emptyList(), "u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }

    @Test
    fun `syncDelete on 200 batch-removes every pending row for the ids sent`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val coordinator = InboxDeleteCoordinator(
            scope, ctApi(200), headerBuilder(), { dbAdapter }, mockk<Logger>(relaxed = true),
            UnconfinedTestDispatcher(testScheduler)
        )

        coordinator.syncDelete(listOf(message("m1"), message("m2"), message("m3")), "u")
        advanceUntilIdle()

        verify(exactly = 1) { dbAdapter.removePendingDeletes(listOf("m1", "m2", "m3"), "u") }
    }

    @Test
    fun `syncDelete on non-2xx leaves every pending row for the next retry`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val coordinator = InboxDeleteCoordinator(
            scope, ctApi(500), headerBuilder(), { dbAdapter }, mockk<Logger>(relaxed = true),
            UnconfinedTestDispatcher(testScheduler)
        )

        coordinator.syncDelete(listOf(message("m1"), message("m2")), "u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }

    @Test
    fun `retryPending drains every pending row for the user in one call`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true).apply {
            every { getPendingDeletes("u") } returns setOf("m1", "m2")
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val coordinator = InboxDeleteCoordinator(
            scope, ctApi(200), headerBuilder(), { dbAdapter }, mockk<Logger>(relaxed = true),
            UnconfinedTestDispatcher(testScheduler)
        )

        coordinator.retryPending("u")
        advanceUntilIdle()

        verify { dbAdapter.removePendingDeletes(match { it.toSet() == setOf("m1", "m2") }, "u") }
    }

    @Test
    fun `retryPending with no pending rows is a no-op`() = runTest {
        val dbAdapter = mockk<DBAdapter>(relaxed = true).apply {
            every { getPendingDeletes("u") } returns emptySet()
        }
        val scope = NetworkScope(StandardTestDispatcher(testScheduler))
        val coordinator = InboxDeleteCoordinator(
            scope, ctApi(200), headerBuilder(), { dbAdapter }, mockk<Logger>(relaxed = true),
            UnconfinedTestDispatcher(testScheduler)
        )

        coordinator.retryPending("u")
        advanceUntilIdle()

        verify(exactly = 0) { dbAdapter.removePendingDeletes(any<List<String>>(), any()) }
    }
}
