package com.clevertap.android.sdk.network.api

import android.net.Uri
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.Request
import com.clevertap.android.sdk.network.http.Response

class CtApi(
    private val httpClient: CtHttpClient,
    val defaultDomain: String,
    var domain: String?,
    var spikyDomain: String?,
    var region: String?,
    var proxyDomain: String?,
    var spikyProxyDomain: String?,
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

    fun sendQueue(useSpikyDomain: Boolean, body: SendQueueRequestBody): Response =
        httpClient.execute(createRequest(
            relativePath = "a1",
            body = body.toString(),
            useSpikyDomain = useSpikyDomain,
            includeTs = true)
        )

    fun performHandshakeForDomain(useSpikyDomain: Boolean): Response {
        val request = createRequest(
            relativePath = "hello",
            body = null,
            useSpikyDomain = useSpikyDomain,
            includeTs = false
        )
        logger.verbose(logTag, "Performing handshake with ${request.url}")
        return httpClient.execute(request)
    }

    fun defineVars(body: SendQueueRequestBody): Response =
        httpClient.execute(
            createRequest("defineVars", body.toString(), useSpikyDomain = false, includeTs = true)
        )

    fun getActualDomain(useSpikyDomain: Boolean): String? {
        return when {
            !region.isNullOrBlank() -> {
                val regionSuffix = if (useSpikyDomain) spikyRegionSuffix else ""
                "$region${regionSuffix}.$defaultDomain"
            }

            !useSpikyDomain && !proxyDomain.isNullOrBlank() -> {
                proxyDomain
            }

            useSpikyDomain && !spikyProxyDomain.isNullOrBlank() -> {
                spikyProxyDomain
            }

            else -> if (useSpikyDomain) {
                spikyDomain
            } else {
                domain
            }
        }
    }

    private fun createRequest(
        relativePath: String,
        body: String?,
        useSpikyDomain: Boolean,
        includeTs: Boolean
    ) = Request(
        url = getUriForPath(path = relativePath, useSpikyDomain = useSpikyDomain, includeTs = includeTs),
        headers = defaultHeaders,
        body = body
    )

    private fun getUriForPath(path: String, useSpikyDomain: Boolean, includeTs: Boolean): Uri {
        val builder = Uri.Builder()
            .scheme("https")
            .authority(getActualDomain(useSpikyDomain) ?: defaultDomain)
            .appendPath(path)
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
