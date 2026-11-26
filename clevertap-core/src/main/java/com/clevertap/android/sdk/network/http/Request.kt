package com.clevertap.android.sdk.network.http

import androidx.annotation.RestrictTo
import android.net.Uri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Request(
    val url: Uri,
    val headers: Map<String, String>,
    val body: String?
)
