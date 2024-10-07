package com.clevertap.android.sdk.network.http

import java.net.HttpURLConnection

class MockHttpClient(
    var responseCode: Int = HttpURLConnection.HTTP_OK,
    var responseHeaders: Map<String, List<String>> = mapOf(),
    var responseBody: String? = ""
) : CtHttpClient {

    var alwaysThrowOnExecute = false

    override fun execute(request: Request): Response {
        if (alwaysThrowOnExecute) {
            throw RuntimeException("MockHttpClient exception on execute")
        }
        return Response(
            request = request,
            code = responseCode,
            headers = responseHeaders,
            bodyStream = responseBody?.byteInputStream(Charsets.UTF_8)
        ) {}
    }
}
