package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.network.DownloadedBitmap

class TestInAppFetchApi(
    private val bitmap: Bitmap?,
    private val status: DownloadedBitmap.Status,
    private val downloadTime: Long,
    private val bytes: ByteArray? = null
) : InAppImageFetchApiContract {

    companion object {
        fun success(bitmap: Bitmap, bytes: ByteArray?) = TestInAppFetchApi(
                bitmap = bitmap,
                status = DownloadedBitmap.Status.SUCCESS,
                downloadTime = 25,
                bytes = bytes
        )
    }
    override fun makeApiCallForInAppBitmap(url: String): DownloadedBitmap {
        return DownloadedBitmap(
            bitmap = bitmap,
            status = status,
            downloadTime = downloadTime,
            bytes = bytes,
        )
    }
}