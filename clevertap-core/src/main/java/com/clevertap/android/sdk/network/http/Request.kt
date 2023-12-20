package com.clevertap.android.sdk.network.http

import android.net.Uri

class Request(val url: Uri, val headers: Map<String, String>, val body: String?)
