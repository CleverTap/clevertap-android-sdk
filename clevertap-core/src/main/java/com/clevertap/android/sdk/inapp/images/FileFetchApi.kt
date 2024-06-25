package com.clevertap.android.sdk.inapp.images

import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.data.CtCacheType.FILES
import com.clevertap.android.sdk.inapp.data.CtCacheType.GIF
import com.clevertap.android.sdk.inapp.data.CtCacheType.IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmap

interface FileFetchApiContract {
    fun makeApiCallForFile(urlMeta: Pair<String,CtCacheType>): DownloadedBitmap
}

class FileFetchApi : FileFetchApiContract {
    override fun makeApiCallForFile(urlMeta: Pair<String, CtCacheType>): DownloadedBitmap {
        val request = BitmapDownloadRequest(urlMeta.first)
        val bitmapOperation = when(urlMeta.second){
            IMAGE,
            GIF -> HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP
            FILES ->  HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_BYTES
        }
        return HttpBitmapLoader.getHttpBitmap(
            bitmapOperation = bitmapOperation,
            bitmapDownloadRequest = request
        )
    }
}