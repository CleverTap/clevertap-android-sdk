package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmapFactory

class NotificationBitmapDownloadRequestHandler(
    private val iBitmapDownloadRequestHandler: IBitmapDownloadRequestHandler
) : IBitmapDownloadRequestHandler {

    override fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap {
        Logger.v("handling bitmap download request in NotificationBitmapDownloadRequestHandler....")

        val (srcUrl, fallbackToAppIcon, context) = bitmapDownloadRequest

        // If the bitmap path is not specified

        if (srcUrl.isNullOrBlank()) {
            return Utils.getDownloadedBitmapPostFallbackIconCheck(
                fallbackToAppIcon, context,
                DownloadedBitmapFactory.nullBitmapWithStatus(NO_IMAGE)
            )
        }

        val downloadedBitmap: DownloadedBitmap = iBitmapDownloadRequestHandler.handleRequest(bitmapDownloadRequest)

        return Utils.getDownloadedBitmapPostFallbackIconCheck(fallbackToAppIcon, context, downloadedBitmap)
    }
}