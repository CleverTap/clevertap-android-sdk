package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches

class InAppImageProvider(
    private val context: Context,
    private val logger: ILogger? = null
) {

    private val ctCaches = CTCaches.instance(logger = logger)

    fun saveImage(cacheKey: String, bitmap: Bitmap, bytes: ByteArray) {

        val imageMemoryCache = ctCaches.imageCache()
        imageMemoryCache.add(cacheKey, bitmap)

        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        imageDiskCache.add(cacheKey, bytes)
    }

    fun cachedImage(cacheKey: String) {

        // Try in memory
        val imageMemoryCache = ctCaches.imageCache()
        val bitmap = imageMemoryCache.get(cacheKey)

        if (bitmap != null) {
            return
        }

        // Try disk
        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        val file = imageDiskCache.get(cacheKey)

        if (file != null) {
            return
        }

    }

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    fun fetchImage(url: String) {
        val request = BitmapDownloadRequest(url)
        val downloadedBitmap: DownloadedBitmap = HttpBitmapLoader.getHttpBitmap(
            bitmapOperation = HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP,
            bitmapDownloadRequest = request
        )

        when (downloadedBitmap.status) {

            DownloadedBitmap.Status.SUCCESS -> {
                saveImage(url, downloadedBitmap.bitmap!!, downloadedBitmap.bytes!!)
            }
            else -> {
                logger?.verbose("There was a problem fetching data for bitmap")
            }
        }
    }

}