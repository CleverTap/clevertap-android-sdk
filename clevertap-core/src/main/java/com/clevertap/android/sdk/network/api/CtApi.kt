package com.clevertap.android.sdk.network.api

import android.net.Uri
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.isNotNullAndBlank
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response

internal class CtApi(
    private val httpClient: CtHttpClient,
    val defaultDomain: String,
    var cachedDomain: String?,
    var cachedSpikyDomain: String?,
    var region: String?,
    var proxyDomain: String?,
    var spikyProxyDomain: String?,
    var customHandshakeDomain: String?,
    accountId: String,
    accountToken: String,
    sdkVersion: String,
    private val logger: Logger,
    private val logTag: String
) {

    companion object {

        const val DEFAULT_CONTENT_TYPE = "application/json; charset=utf-8"
        const val DEFAULT_QUERY_PARAM_OS = "Android"

        // Request Headers
        const val HEADER_CUSTOM_HANDSHAKE = "X-CleverTap-Handshake-Domain"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_ACCOUNT_ID = "X-CleverTap-Account-ID"
        const val HEADER_ACCOUNT_TOKEN = "X-CleverTap-Token"
        const val HEADER_ENCRYPTION_ENABLED = "X-CleverTap-Encryption-Enabled"

        // Response Headers
        const val HEADER_MUTE: String = "X-WZRK-MUTE"
        const val HEADER_DOMAIN_NAME: String = "X-WZRK-RD"
        const val SPIKY_HEADER_DOMAIN_NAME: String = "X-WZRK-SPIKY-RD"
    }

    private val defaultHeaders: Map<String, String> = mapOf(
        HEADER_CONTENT_TYPE to DEFAULT_CONTENT_TYPE,
        HEADER_ACCOUNT_ID to accountId,
        HEADER_ACCOUNT_TOKEN to accountToken
    )
    private val defaultQueryParams: Map<String, String> = mapOf(
        "os" to DEFAULT_QUERY_PARAM_OS,
        "t" to sdkVersion,
        "z" to accountId
    )
    private val encryptionHeader = HEADER_ENCRYPTION_ENABLED to "true"

    private val spikyRegionSuffix = "-spiky"
    var currentRequestTimestampSeconds = 0
        private set

    fun sendQueue(
        isViewedEvent: Boolean,
        body: String,
        isEncrypted: Boolean = false
    ): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(isViewedEvent = isViewedEvent) ?: defaultDomain,
                relativeUrl = "a1",
                body = body,
                headers = if (isEncrypted) {
                    defaultHeaders.plus(encryptionHeader)
                } else {
                    defaultHeaders
                }
            )
        )

    fun performHandshakeForDomain(isViewedEvent: Boolean): Response {
        val baseUrl = getHandshakeDomain(isViewedEvent)

        // append extra info in header in-case we are using custom handshake domain
        val headers = if (customHandshakeDomain.isNotNullAndBlank() && baseUrl == customHandshakeDomain) {
            defaultHeaders.plus(HEADER_CUSTOM_HANDSHAKE to customHandshakeDomain!!)
        } else {
            defaultHeaders
        }
        val request = createRequest(
            baseUrl = baseUrl,
            relativeUrl = "hello",
            body = null,
            includeTs = false,
            headers = headers
        )

        logger.verbose(logTag, "Performing handshake with ${request.url}")

        return httpClient.execute(request)
    }

    fun defineVars(body: SendQueueRequestBody): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(isViewedEvent = false) ?: defaultDomain,
                relativeUrl = "defineVars",
                body = body.toString()
            )
        )

    fun defineTemplates(body: DefineTemplatesRequestBody): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(isViewedEvent = false) ?: defaultDomain,
                relativeUrl = "defineTemplates",
                body = body.toString()
            )
        )

    fun getActualDomain(isViewedEvent: Boolean): String? {

        if (region.isNotNullAndBlank()) {
            return buildString {
                append(region)
                append(
                    if (isViewedEvent) {
                        spikyRegionSuffix
                    } else {
                        ""
                    }
                )
                append(".")
                append(defaultDomain)
            }
        }

        val toCheckProxy = if (isViewedEvent) { spikyProxyDomain } else { proxyDomain }
        if (toCheckProxy.isNotNullAndBlank()) {
            return toCheckProxy
        }

        val toCheckCached = if (isViewedEvent) { cachedSpikyDomain } else { cachedDomain }
        return toCheckCached
    }

    fun getHandshakeDomain(isViewedEvent: Boolean) : String {
        if (region.isNotNullAndBlank()) {
            return buildString {
                append(region)
                append(
                    if (isViewedEvent) {
                        spikyRegionSuffix
                    } else {
                        ""
                    }
                )
                append(".")
                append(defaultDomain)
            }
        }

        val toCheckProxy = if (isViewedEvent) { spikyProxyDomain } else { proxyDomain }
        if (toCheckProxy.isNotNullAndBlank()) {
            return toCheckProxy
        }

        if (customHandshakeDomain.isNotNullAndBlank()) {
            return customHandshakeDomain!!
        }

        val toCheckCached = if (isViewedEvent) { cachedSpikyDomain } else { cachedDomain }
        if (toCheckCached.isNotNullAndBlank()) {
            return toCheckCached
        }

        return defaultDomain
    }

    fun needsHandshake(isViewedEvent: Boolean) : Boolean {

        if (region.isNotNullAndBlank()) {
            return false
        }

        val toCheckProxy = if (isViewedEvent) { spikyProxyDomain } else { proxyDomain }
        if (toCheckProxy.isNotNullAndBlank()) {
            return false
        }

        val toCheckCached = if (isViewedEvent) { cachedSpikyDomain } else { cachedDomain }
        return toCheckCached.isNullOrBlank()
    }

    private fun createRequest(
        baseUrl: String,
        relativeUrl: String,
        body: String?,
        includeTs: Boolean = true,
        headers: Map<String, String> = defaultHeaders
    ) = Request(
        url = getUriForPath(
            baseUrl = baseUrl,
            relativeUrl = relativeUrl,
            includeTs = includeTs
        ),
        headers = headers,
        body = body
    )

    private fun getUriForPath(
        baseUrl: String,
        relativeUrl: String,
        includeTs: Boolean
    ): Uri {
        val builder = Uri.Builder()
            .scheme("https")
            .authority(baseUrl)
            .appendPath(relativeUrl)
            .appendDefaultQueryParams()
        if (includeTs) {
            builder.appendTsQueryParam()
        }
        return builder.build()
    }

    private fun Uri.Builder.appendDefaultQueryParams(): Uri.Builder {
        for (queryParam in defaultQueryParams) {
            appendQueryParameter(queryParam.key, queryParam.value)
        }
        return this
    }

    private fun Uri.Builder.appendTsQueryParam(): Uri.Builder {
        currentRequestTimestampSeconds = (System.currentTimeMillis() / 1000).toInt()
        return appendQueryParameter("ts", currentRequestTimestampSeconds.toString())
    }
}