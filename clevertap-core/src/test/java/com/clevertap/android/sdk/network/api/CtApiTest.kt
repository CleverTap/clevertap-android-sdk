package com.clevertap.android.sdk.network.api

import org.json.JSONArray
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CtApiTest {

    private lateinit var ctApi: CtApi

    @Before
    fun setUpApi() {
        ctApi = CtApiTestProvider.provideDefaultTestCtApi()
    }

    @Test
    fun test_sendRequests_alwaysAttachDefaultHeaders() {
        val expectedHeaders = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "X-CleverTap-Account-ID" to CtApiTestProvider.ACCOUNT_ID,
            "X-CleverTap-Token" to CtApiTestProvider.ACCOUNT_TOKEN
        )

        val sendQueueResponse = ctApi.sendQueue(false, getEmptyQueueBody())
        assertEquals(expectedHeaders, sendQueueResponse.request.headers)

        val handshakeResponse = ctApi.performHandshakeForDomain(false)
        assertEquals(expectedHeaders, handshakeResponse.request.headers)

        val sendVarsResponse = ctApi.defineVars(getEmptyQueueBody())
        assertEquals(expectedHeaders, sendVarsResponse.request.headers)
    }

    @Test
    fun test_sendQueueAndVariables_updateCurrentRequestTimestamp() {
        ctApi.sendQueue(true, getEmptyQueueBody())
        val timestamp = ctApi.currentRequestTimestampSeconds
        Thread.sleep(1000)
        ctApi.defineVars(getEmptyQueueBody())
        assertNotEquals(timestamp, ctApi.currentRequestTimestampSeconds)
    }

    @Test
    fun test_sendQueue_attachDefaultQueryParams() {
        val request = ctApi.sendQueue(false, getEmptyQueueBody()).request
        val urlString = request.url.toString()
        assertContains(urlString, "os=Android")
        assertContains(urlString, "t=${CtApiTestProvider.SDK_VERSION}")
        assertContains(urlString, "z=${CtApiTestProvider.ACCOUNT_ID}")
        assertContains(urlString, "ts=${ctApi.currentRequestTimestampSeconds}")
    }

    @Test
    fun test_getActualDomain_definedRegion_returnFormattedDefaultDomains() {
        assertEquals("${CtApiTestProvider.REGION}.${CtApiTestProvider.DEFAULT_DOMAIN}", ctApi.getActualDomain(false))
        assertEquals(
            "${CtApiTestProvider.REGION}-spiky.${CtApiTestProvider.DEFAULT_DOMAIN}",
            ctApi.getActualDomain(true)
        )
    }

    @Test
    fun test_getActualDomain_noRegion_returnProxies() {
        ctApi.region = null
        assertEquals(CtApiTestProvider.PROXY_DOMAIN, ctApi.getActualDomain(false))
        assertEquals(CtApiTestProvider.SPIKY_PROXY_DOMAIN, ctApi.getActualDomain(true))
    }

    @Test
    fun test_getActualDomain_noRegion_noProxies_returnSavedDomains() {
        ctApi.region = null
        ctApi.proxyDomain = null
        ctApi.spikyProxyDomain = null
        assertEquals(CtApiTestProvider.DOMAIN, ctApi.getActualDomain(false))
        assertEquals(CtApiTestProvider.SPIKY_DOMAIN, ctApi.getActualDomain(true))
    }

    private fun getEmptyQueueBody(): SendQueueRequestBody {
        return SendQueueRequestBody(null, JSONArray())
    }
}
