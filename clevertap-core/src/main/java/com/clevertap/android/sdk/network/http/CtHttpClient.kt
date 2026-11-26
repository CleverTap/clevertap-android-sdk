package com.clevertap.android.sdk.network.http

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun interface CtHttpClient {

    fun execute(request: Request): Response
}
