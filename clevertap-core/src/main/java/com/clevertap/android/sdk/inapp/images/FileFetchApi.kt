package com.clevertap.android.sdk.inapp.images

import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.data.CtCacheType.FILES
import com.clevertap.android.sdk.inapp.data.CtCacheType.GIF
import com.clevertap.android.sdk.inapp.data.CtCacheType.IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import com.clevertap.android.sdk.network.NetworkMonitor

interface FileFetchApiContract {
    fun makeApiCallForFile(urlMeta: Pair<String,CtCacheType>): DownloadedBitmap
}

internal class FileFetchApi(
    private val networkMonitor: NetworkMonitor? = null
) : FileFetchApiContract {
    override fun makeApiCallForFile(urlMeta: Pair<String, CtCacheType>): DownloadedBitmap {
        if (networkMonitor != null && !networkMonitor.isNetworkOnline()) {
            return DownloadedBitmapFactory.nullBitmapWithStatus(DownloadedBitmap.Status.NO_NETWORK)
        }
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