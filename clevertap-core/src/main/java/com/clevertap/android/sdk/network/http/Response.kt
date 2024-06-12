package com.clevertap.android.sdk.network.http

import java.io.Closeable
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection

class Response(
    val request: Request,
    val code: Int,
    val headers: Map<String, List<String>>,
    bodyStream: InputStream?,
    private val closeDelegate: () -> Unit
) : Closeable {

    private val bodyReader: Reader? = bodyStream?.bufferedReader(Charsets.UTF_8)

    fun isSuccess(): Boolean = code == HttpURLConnection.HTTP_OK

    fun getHeaderValue(header: String): String? = headers[header]?.lastOrNull()

    fun readBody(): String? {
        return bodyReader?.readText()
    }

    override fun close() {
        bodyReader?.close()
        closeDelegate()
    }
}
