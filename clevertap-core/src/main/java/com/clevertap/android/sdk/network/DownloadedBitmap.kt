package com.clevertap.android.sdk.network

import android.graphics.Bitmap

/**
 * Represents a downloaded bitmap with its associated status and download time.
 *
 * @property bitmap The downloaded bitmap. Can be null if the download was unsuccessful.
 * @property status The status of the downloaded bitmap.
 * @property downloadTime The time taken to download the bitmap, in milliseconds.
 */
data class DownloadedBitmap(
    var bitmap: Bitmap?,
    var status: Status,
    var downloadTime: Long
) {

    /**
     * Enum class representing the status of a downloaded bitmap.
     *
     * @property statusValue The value associated with the status.
     */
    enum class Status(val statusValue: String) {
        NO_IMAGE("NO_IMAGE"),
        SUCCESS("SUCCESS"),
        DOWNLOAD_FAILED("DOWNLOAD_FAILED"),
        NO_NETWORK("NO_NETWORK"),
        INIT_ERROR("INIT_ERROR")
    }
}
