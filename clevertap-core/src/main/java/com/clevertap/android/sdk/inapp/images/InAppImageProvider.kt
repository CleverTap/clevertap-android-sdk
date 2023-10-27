package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation
import com.clevertap.android.sdk.utils.CTCaches

class InAppImageProvider(
    private val context: Context,
    private val logger: ILogger? = null
) {

    fun saveImage(cacheKey: String, bitmap: Bitmap) {
        val ctCaches = CTCaches.instance(logger = logger)

        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        val imageMemoryCache = ctCaches.imageCache()

        imageMemoryCache.add(cacheKey, bitmap)
        //imageDiskCache.add(cacheKey, bitmap)
    }

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    fun fetchImage(url: String) {
        val request = BitmapDownloadRequest(url)
        val downloadedBitmap = HttpBitmapLoader.getHttpBitmap(
            bitmapOperation = HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP,
            bitmapDownloadRequest = request
        )

    }


}

fun Bitmap.toByteArray() {

}