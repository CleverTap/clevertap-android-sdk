package com.clevertap.android.sdk.network

import android.graphics.Bitmap

/**
 * Represents a downloaded bitmap with its associated status and download time.
 *
 * @property bitmap The downloaded bitmap. Can be null if the download was unsuccessful.
 * @property status The status of the downloaded bitmap.
 * @property downloadTime The time taken to download the bitmap, in milliseconds.
 */
data class DownloadedBitmap constructor(
    val bitmap: Bitmap?,
    val status: Status,
    val downloadTime: Long,
    val bytes: ByteArray? = null
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
        INIT_ERROR("INIT_ERROR"),
        SIZE_LIMIT_EXCEEDED("SIZE_LIMIT_EXCEEDED")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadedBitmap

        if (bitmap != other.bitmap) return false
        if (status != other.status) return false
        if (downloadTime != other.downloadTime) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitmap?.hashCode() ?: 0
        result = 31 * result + status.hashCode()
        result = 31 * result + downloadTime.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
