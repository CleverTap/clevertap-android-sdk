package com.clevertap.android.sdk.network.api

import android.net.Uri
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.images.isNotNullAndEmpty
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response

internal class CtApi(
    private val httpClient: CtHttpClient,
    val defaultDomain: String,
    var cachedDomain: String?,
    var cachedSpikyDomain: String?,
    var cachedHandshakeDomain: String?,
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
    }

    private val defaultHeaders: Map<String, String> = mapOf(
        "Content-Type" to DEFAULT_CONTENT_TYPE,
        "X-CleverTap-Account-ID" to accountId,
        "X-CleverTap-Token" to accountToken
    )
    private val defaultQueryParams: Map<String, String> = mapOf(
        "os" to DEFAULT_QUERY_PARAM_OS,
        "t" to sdkVersion,
        "z" to accountId
    )
    private val spikyRegionSuffix = "-spiky"
    var currentRequestTimestampSeconds = 0
        private set

    fun sendQueue(
        useSpikyDomain: Boolean,
        body: SendQueueRequestBody
    ): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(useSpikyDomain) ?: defaultDomain,
                relativeUrl = "a1",
                body = body.toString(),
                includeTs = true
            )
        )

    fun performHandshakeForDomain(useSpikyDomain: Boolean): Response {
        val request = createRequest(
            baseUrl = getHandshakeDomain(useSpikyDomain),
            relativeUrl = "hello",
            body = null,
            includeTs = false
        )
        logger.verbose(logTag, "Performing handshake with ${request.url}")
        return httpClient.execute(request)
    }

    fun defineVars(body: SendQueueRequestBody): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(isViewedEvent = false) ?: defaultDomain,
                relativeUrl = "defineVars",
                body = body.toString(),
                includeTs = true
            )
        )

    fun defineTemplates(body: DefineTemplatesRequestBody): Response =
        httpClient.execute(
            createRequest(
                baseUrl = getActualDomain(isViewedEvent = false) ?: defaultDomain,
                relativeUrl = "defineTemplates",
                body = body.toString(),
                includeTs = true
            )
        )

    fun getActualDomain(isViewedEvent: Boolean): String? {
        return when {
            !region.isNullOrBlank() -> {
                val regionSuffix = if (isViewedEvent) {
                    spikyRegionSuffix
                } else {
                    ""
                }
                buildString {
                    append(region)
                    append(regionSuffix)
                    append(".")
                    append(defaultDomain)
                }
            }

            !isViewedEvent && !proxyDomain.isNullOrBlank() -> {
                proxyDomain
            }

            isViewedEvent && !spikyProxyDomain.isNullOrBlank() -> {
                spikyProxyDomain
            }

            else -> if (isViewedEvent) {
                cachedSpikyDomain
            } else {
                cachedDomain
            }
        }
    }

    fun getHandshakeDomain(isViewedEvent: Boolean) : String {
        if (region.isNotNullAndEmpty()) {
            val regionSuffix = if (isViewedEvent) {
                spikyRegionSuffix
            } else {
                ""
            }
            return buildString {
                append(region)
                append(regionSuffix)
                append(".")
                append(defaultDomain)
            }
        }

        if (isViewedEvent) {
            if (spikyProxyDomain.isNotNullAndEmpty()) {
                return spikyProxyDomain!!
            }
        } else {
            if (proxyDomain.isNotNullAndEmpty()) {
                return proxyDomain!!
            }
        }

        if (customHandshakeDomain.isNotNullAndEmpty()) {
            return customHandshakeDomain!!
        }

        if (isViewedEvent) {
            if (cachedSpikyDomain.isNotNullAndEmpty()) {
                return cachedSpikyDomain!!
            }
        } else {
            if (cachedDomain.isNotNullAndEmpty()) {
                return cachedDomain!!
            }
        }

        return defaultDomain
    }

    fun needsHandshake(isViewedEvent: Boolean) : Boolean {

        if (region.isNullOrBlank().not()) {
            return false
        }

        if (isViewedEvent) {
            if (spikyProxyDomain.isNullOrBlank().not()) {
                return false
            }
        } else {
            if (proxyDomain.isNullOrBlank().not()) {
                return false
            }
        }

        if (customHandshakeDomain.isNullOrBlank().not()) {
            return cachedHandshakeDomain.isNullOrBlank()
        }

        return if (isViewedEvent) {
            cachedSpikyDomain.isNullOrBlank()
        } else {
            cachedDomain.isNullOrBlank()
        }
    }

    private fun createRequest(
        baseUrl: String,
        relativeUrl: String,
        body: String?,
        includeTs: Boolean
    ) = Request(
        url = getUriForPath(
            baseUrl = baseUrl,
            relativeUrl = relativeUrl,
            includeTs = includeTs
        ),
        headers = defaultHeaders,
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
