package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.network.DownloadedBitmap

class TestInAppFetchApi(
    private val bitmap: Bitmap?,
    private val status: DownloadedBitmap.Status,
    private val downloadTime: Long,
    private val bytes: ByteArray? = null
) : FileFetchApiContract {

    companion object {
        fun success(bitmap: Bitmap, bytes: ByteArray?) = TestInAppFetchApi(
                bitmap = bitmap,
                status = DownloadedBitmap.Status.SUCCESS,
                downloadTime = 25,
                bytes = bytes
        )
    }
    override fun makeApiCallForFile(urlMeta: Pair<String, CtCacheType>): DownloadedBitmap {
        return DownloadedBitmap(
            bitmap = bitmap,
            status = status,
            downloadTime = downloadTime,
            bytes = bytes,
        )
    }
}