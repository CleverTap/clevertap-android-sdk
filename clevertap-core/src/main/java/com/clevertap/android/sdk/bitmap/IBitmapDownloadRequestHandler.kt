package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.network.DownloadedBitmap

interface IBitmapDownloadRequestHandler {
    fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap
}