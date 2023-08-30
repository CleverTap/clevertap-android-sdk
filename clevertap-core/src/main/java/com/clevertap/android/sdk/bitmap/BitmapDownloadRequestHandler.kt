package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_NETWORK
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import com.clevertap.android.sdk.network.NetworkManager

open class BitmapDownloadRequestHandler(private val bitmapDownloader: BitmapDownloader) :
    IBitmapDownloadRequestHandler {

    override fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap {
        Logger.verbose("handling bitmap download request in BitmapDownloadRequestHandler....")

        var srcUrl = bitmapDownloadRequest.bitmapPath
        val context = bitmapDownloadRequest.context

        // If the bitmap path is not specified
        if (srcUrl.isNullOrBlank()) {
            return DownloadedBitmapFactory.nullBitmapWithStatus(NO_IMAGE)
        }

        // Safe bet, won't have more than three /s . url must not be null since we are not handling null pointer exception that would cause otherwise
        srcUrl = srcUrl.replace("///", "/")
            .replace("//", "/")
            .replace("http:/", "http://")
            .replace("https:/", "https://")

        context?.run {
            val isNetworkOnline = NetworkManager.isNetworkOnline(this)
            if (!isNetworkOnline) {
                Logger.verbose("Network connectivity unavailable. Not downloading bitmap. URL was: $srcUrl")
                return DownloadedBitmapFactory.nullBitmapWithStatus(NO_NETWORK)
            }
        }

        return bitmapDownloader.downloadBitmap(srcUrl)
    }
}