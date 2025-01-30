package com.clevertap.android.sdk.network.http

import java.net.HttpURLConnection

class Response(
    val request: Request,
    val code: Int,
    val headers: Map<String, List<String>>,
    val body: String?
) {

    fun isSuccess(): Boolean = code == HttpURLConnection.HTTP_OK

    fun getHeaderValue(header: String): String? = headers[header]?.lastOrNull()

    fun readBody(): String? {
        return body
    }
}
