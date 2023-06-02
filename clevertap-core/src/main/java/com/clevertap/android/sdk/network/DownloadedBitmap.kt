package com.clevertap.android.sdk.network

import android.graphics.Bitmap

data class DownloadedBitmap(
    var bitmap: Bitmap?,
    var status: Status,
    var downloadTime: Long
) {
    enum class Status(val statusValue: String) {
        NO_IMAGE("NO_IMAGE"),
        SUCCESS("SUCCESS"),
        DOWNLOAD_FAILED("DOWNLOAD_FAILED"),
        NO_NETWORK("NO_NETWORK"),
        ERROR("ERROR")
    }
}
