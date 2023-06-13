package com.clevertap.android.sdk.network

import android.graphics.Bitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.SUCCESS

/**
 * Factory class for creating instances of DownloadedBitmap.
 */
object DownloadedBitmapFactory {

    /**
     * Creates a DownloadedBitmap instance with a null bitmap and the provided status.
     *
     * @param status The status indicating the result of the download operation.
     * @return A DownloadedBitmap instance with a null bitmap and the provided status.
     */
    fun nullBitmapWithStatus(status: Status): DownloadedBitmap {
        return DownloadedBitmap(null, status, -1)
    }


    /**
     * Creates a DownloadedBitmap object with the specified bitmap, success status, and download time.
     *
     * @param bitmap      The downloaded bitmap.
     * @param downloadTime The time taken for the download operation in millis.
     * @return The DownloadedBitmap object with the specified bitmap, success status, and download time.
     */
    fun successBitmap(bitmap: Bitmap, downloadTime: Long): DownloadedBitmap {
        return DownloadedBitmap(bitmap, SUCCESS, downloadTime)
    }
}
