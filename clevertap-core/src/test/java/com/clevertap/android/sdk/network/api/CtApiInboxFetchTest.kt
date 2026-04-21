package com.clevertap.android.sdk.network.api

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContains
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CtApiInboxFetchTest {

    private lateinit var ctApi: CtApi

    @Before
    fun setUp() {
        ctApi = CtApiTestProvider.provideDefaultTestCtApi()
    }

    @Test
    fun `sendInboxFetch targets inbox v2 getMessages path with unencoded slashes`() {
        val url = ctApi.sendInboxFetch(body = "[]").request.url.toString()
        assertContains(url, "/inbox/v2/getMessages")
    }

    @Test
    fun `sendInboxFetch forwards body verbatim`() {
        val payload = "[{\"evtName\":\"wzrk_fetch\"}]"
        val request = ctApi.sendInboxFetch(body = payload).request
        assertEquals(payload, request.body)
    }

    @Test
    fun `sendInboxFetch attaches default headers`() {
        val expected = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "X-CleverTap-Account-ID" to CtApiTestProvider.ACCOUNT_ID,
            "X-CleverTap-Token" to CtApiTestProvider.ACCOUNT_TOKEN
        )
        val response = ctApi.sendInboxFetch(body = "[]")
        assertEquals(expected, response.request.headers)
    }

    @Test
    fun `sendInboxFetch attaches default query params`() {
        val url = ctApi.sendInboxFetch(body = "[]").request.url.toString()
        assertContains(url, "os=Android")
        assertContains(url, "t=${CtApiTestProvider.SDK_VERSION}")
        assertContains(url, "z=${CtApiTestProvider.ACCOUNT_ID}")
        assertContains(url, "ts=${ctApi.currentRequestTimestampSeconds}")
    }

    @Test
    fun `sendInboxDelete targets the same inbox v2 path pending backend confirmation`() {
        val url = ctApi.sendInboxDelete(body = "[]").request.url.toString()
        assertContains(url, "/inbox/v2/getMessages")
    }

    @Test
    fun `sendInboxDelete forwards body verbatim`() {
        val payload = "[{\"evtName\":\"Message Deleted\"}]"
        val request = ctApi.sendInboxDelete(body = payload).request
        assertEquals(payload, request.body)
    }

    @Test
    fun `sendInboxDelete attaches default headers`() {
        val expected = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "X-CleverTap-Account-ID" to CtApiTestProvider.ACCOUNT_ID,
            "X-CleverTap-Token" to CtApiTestProvider.ACCOUNT_TOKEN
        )
        val response = ctApi.sendInboxDelete(body = "[]")
        assertEquals(expected, response.request.headers)
    }
}
