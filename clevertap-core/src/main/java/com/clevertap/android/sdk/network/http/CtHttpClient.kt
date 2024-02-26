package com.clevertap.android.sdk.network.http

fun interface CtHttpClient {

    fun execute(request: Request): Response
}
