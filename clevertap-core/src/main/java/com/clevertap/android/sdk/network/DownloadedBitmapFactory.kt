package com.clevertap.android.sdk.network

import android.graphics.Bitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.SUCCESS

object DownloadedBitmapFactory {

    fun nullBitmapWithStatus(status: Status): DownloadedBitmap {
        return DownloadedBitmap(null, status, -1)
    }

    fun successBitmap(bitmap: Bitmap, downloadTime: Long): DownloadedBitmap {
        return DownloadedBitmap(bitmap, SUCCESS, downloadTime)
    }
}
