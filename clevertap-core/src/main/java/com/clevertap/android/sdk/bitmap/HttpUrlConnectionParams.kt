package com.clevertap.android.sdk.bitmap

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HttpUrlConnectionParams @JvmOverloads constructor(
    var connectTimeout: Int = 0,
    var readTimeout: Int = 0,
    var useCaches: Boolean = false,
    var doInput: Boolean = false,
    var requestMap: Map<String,String> = mapOf()
)