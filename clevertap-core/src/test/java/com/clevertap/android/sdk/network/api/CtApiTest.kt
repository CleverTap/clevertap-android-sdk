package com.clevertap.android.sdk.network.api

import org.json.JSONArray
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

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

        val sendQueueResponse = ctApi.sendQueue(false, getEmptyQueueBody().toString(), false)
        assertEquals(expectedHeaders, sendQueueResponse.request.headers)

        val handshakeResponse = ctApi.performHandshakeForDomain(false)
        assertEquals(expectedHeaders, handshakeResponse.request.headers)

        val sendVarsResponse = ctApi.defineVars(getEmptyQueueBody())
        assertEquals(expectedHeaders, sendVarsResponse.request.headers)
    }

    @Test
    fun `test hello request attaches extra header in case custom domain is mentioned in manifest`() {

        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
        }

        // expect
        val expectedHeaders = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "X-CleverTap-Account-ID" to CtApiTestProvider.ACCOUNT_ID,
            "X-CleverTap-Token" to CtApiTestProvider.ACCOUNT_TOKEN,
            CtApi.HEADER_CUSTOM_HANDSHAKE to CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
        )

        // Perform
        val handshakeResponse = ctApi.performHandshakeForDomain(false)

        // Assertions
        assertEquals(expectedHeaders, handshakeResponse.request.headers)
        assertNull(handshakeResponse.request.body)
        assertEquals(
            expected = "https://${CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN}/hello?os=Android&t=${CtApiTestProvider.SDK_VERSION}&z=${CtApiTestProvider.ACCOUNT_ID}",
            actual = handshakeResponse.request.url.toString()
        )

        // Perform
        val handshakeResponseSpiky = ctApi.performHandshakeForDomain(true)

        // Assertions
        assertEquals(expectedHeaders, handshakeResponseSpiky.request.headers)
        assertNull(handshakeResponseSpiky.request.body)
        assertEquals(
            expected = "https://${CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN}/hello?os=Android&t=${CtApiTestProvider.SDK_VERSION}&z=${CtApiTestProvider.ACCOUNT_ID}",
            actual = handshakeResponseSpiky.request.url.toString()
        )
    }

    @Test
    fun test_sendQueueAndVariables_updateCurrentRequestTimestamp() {
        ctApi.sendQueue(true, getEmptyQueueBody().toString(), false)
        val timestamp = ctApi.currentRequestTimestampSeconds
        Thread.sleep(1000)
        ctApi.defineVars(getEmptyQueueBody())
        assertNotEquals(timestamp, ctApi.currentRequestTimestampSeconds)
    }

    @Test
    fun test_sendQueue_attachDefaultQueryParams() {
        val request = ctApi.sendQueue(false, getEmptyQueueBody().toString(), false).request
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
        assertEquals(CtApiTestProvider.CACHED_DOMAIN, ctApi.getActualDomain(false))
        assertEquals(CtApiTestProvider.CACHED_SPIKY_DOMAIN, ctApi.getActualDomain(true))
    }

    @Test
    fun `test get handshake domain returns domain mentioned in manifest, case1-CLEVERTAP_HANDSHAKE_DOMAIN mentioned`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
        }

        // assert
        assertEquals(CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN, ctApi.getHandshakeDomain(false))
        assertEquals(CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN, ctApi.getHandshakeDomain(true))
    }

    @Test
    fun `test get handshake domain returns correct domains if custom domains are mentioned, case2-CLEVERTAP_SPIKY_PROXY_DOMAIN,CLEVERTAP_PROXY_DOMAIN`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = CtApiTestProvider.PROXY_DOMAIN
            spikyProxyDomain = null
            customHandshakeDomain = null
        }

        // assert
        assertEquals(CtApiTestProvider.PROXY_DOMAIN, ctApi.getHandshakeDomain(false))
        assertEquals(CtApiTestProvider.CACHED_SPIKY_DOMAIN, ctApi.getHandshakeDomain(true))

        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = CtApiTestProvider.SPIKY_PROXY_DOMAIN
            customHandshakeDomain = null
        }

        // assert
        assertEquals(CtApiTestProvider.CACHED_DOMAIN, ctApi.getHandshakeDomain(false))
        assertEquals(CtApiTestProvider.SPIKY_PROXY_DOMAIN, ctApi.getHandshakeDomain(true))
    }

    @Test
    fun `test get handshake domain returns correct domains if custom domains are mentioned, case3-Only account id and token are mentioned`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = null
            cachedDomain = null
            cachedSpikyDomain = null
        }

        // assert
        assertEquals(CtApiTestProvider.DEFAULT_DOMAIN, ctApi.getHandshakeDomain(false))

    }

    @Test
    fun `test needs handshake returns false if region is mentioned in manifest`() {
        // setup
        with (ctApi) {
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = null
        }

        // assert
        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(false)
        )

        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(true)
        )
    }

    @Test
    fun `test needs handshake returns false if proxy and spiky proxy domains are mentioned in manifest`() {
        // setup
        with (ctApi) {
            region = null
            customHandshakeDomain = null
        }

        // assert
        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(false)
        )

        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(true)
        )
    }

    @Test
    fun `test needs handshake returns true if handshake domain if first time in manifest and not yet cached`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
            cachedDomain = null
            cachedSpikyDomain = null
        }

        // assert
        assertEquals(
            expected = true,
            actual = ctApi.needsHandshake(false)
        )

        assertEquals(
            expected = true,
            actual = ctApi.needsHandshake(true)
        )
    }

    @Test
    fun `test needs handshake returns false if handshake domain is mentioned first time in manifest after app upgrade`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
            cachedDomain = CtApiTestProvider.CACHED_DOMAIN // will exist in old app
            cachedSpikyDomain = CtApiTestProvider.CACHED_SPIKY_DOMAIN // will exist in old app
        }

        // assert
        assertFalse(ctApi.needsHandshake(false))
        assertFalse(ctApi.needsHandshake(true))
    }

    @Test
    fun `test needs handshake returns false if handshake domain is cached previously`() {
        // setup
        with (ctApi) {
            region = null
            proxyDomain = null
            spikyProxyDomain = null
            customHandshakeDomain = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN
            cachedDomain = "some-cached-domain.com"
            cachedSpikyDomain = "some-cached-spiky-domain.com"
        }

        // assert
        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(false)
        )

        assertEquals(
            expected = false,
            actual = ctApi.needsHandshake(true)
        )

        assertEquals(
            expected = "some-cached-domain.com",
            actual = ctApi.getActualDomain(false)
        )

        assertEquals(
            expected = "some-cached-spiky-domain.com",
            actual = ctApi.getActualDomain(true)
        )

        assertEquals(
            expected = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN,
            actual = ctApi.getHandshakeDomain(false)
        )

        assertEquals(
            expected = CtApiTestProvider.CUSTOM_HANDSHAKE_DOMAIN,
            actual = ctApi.getHandshakeDomain(true)
        )
    }

    private fun getEmptyQueueBody(): SendQueueRequestBody {
        return SendQueueRequestBody(null, JSONArray())
    }
}
