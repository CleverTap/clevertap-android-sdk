package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_NETWORK
import com.clevertap.android.sdk.network.DownloadedBitmapFactory

open class BitmapDownloadRequestHandler(
    private val bitmapDownloader: BitmapDownloader,
    private val isNetworkOnline: () -> Boolean = { true }
) : IBitmapDownloadRequestHandler {

    override fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap {
        Logger.v("handling bitmap download request in BitmapDownloadRequestHandler....")

        var srcUrl = bitmapDownloadRequest.bitmapPath

        // If the bitmap path is not specified
        if (srcUrl.isNullOrBlank()) {
            return DownloadedBitmapFactory.nullBitmapWithStatus(NO_IMAGE)
        }

        // Safe bet, won't have more than three /s . url must not be null since we are not handling null pointer exception that would cause otherwise
        srcUrl = srcUrl.replace("///", "/")
            .replace("//", "/")
            .replace("http:/", "http://")
            .replace("https:/", "https://")

        if (!isNetworkOnline()) {
            Logger.v("Network connectivity unavailable. Not downloading bitmap. URL was: $srcUrl")
            return DownloadedBitmapFactory.nullBitmapWithStatus(NO_NETWORK)
        }

        return bitmapDownloader.downloadBitmap(srcUrl)
    }
}
