package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches
import java.io.ByteArrayOutputStream

class InAppImageProvider(
    val context: Context,
    val logger: ILogger? = null
) {

    private val ctCaches = CTCaches.instance(logger = logger)

    fun saveImage(cacheKey: String, bitmap: Bitmap, bytes: ByteArray) {

        val imageMemoryCache = ctCaches.imageCache()
        imageMemoryCache.add(cacheKey, bitmap)

        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        imageDiskCache.add(cacheKey, bytes)
    }

    fun cachedImage(cacheKey: String): Bitmap? {

        // Try in memory
        val imageMemoryCache = ctCaches.imageCache()
        val bitmap = imageMemoryCache.get(cacheKey)

        if (bitmap != null) {
            return bitmap
        }

        // Try disk
        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        val file = imageDiskCache.get(cacheKey)

        if (file != null) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            // use options to scale down/optimise bitmap if needed
            val op = BitmapFactory.decodeFile(file.absolutePath)
            return op
            /*val clazz = T::class.java
            if (clazz.isAssignableFrom(Bitmap::class.java)) {
                val op = BitmapFactory.decodeFile(file.absolutePath, null)
                return op as T
            } else if (clazz.isAssignableFrom(ByteArray::class.java)) {
                file.inputStream()
            }*/
        }
        return null
    }

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    inline fun <reified T> fetchImage(url: String): T? {

        val cachedImage: Bitmap? = cachedImage(url)
        val clazz = T::class.java

        if (cachedImage != null) {
            return if (clazz.isAssignableFrom(Bitmap::class.java)) {
                cachedImage as? T
            } else if (clazz.isAssignableFrom(ByteArray::class.java)) {
                val stream = ByteArrayOutputStream()
                cachedImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                byteArray as? T
            } else {
                null
            }
        }

        val downloadedBitmap = makeApiCallForInAppBitmap(url = url)

        when (downloadedBitmap.status) {

            DownloadedBitmap.Status.SUCCESS -> {
                saveImage(url, downloadedBitmap.bitmap!!, downloadedBitmap.bytes!!)
            }
            else -> {
                logger?.verbose("There was a problem fetching data for bitmap")
                return null
            }
        }

        return if (clazz.isAssignableFrom(Bitmap::class.java)) {
            downloadedBitmap.bitmap as? T
        } else if (clazz.isAssignableFrom(ByteArray::class.java)) {
            downloadedBitmap.bytes as? T
        } else {
            null
        }
    }

    fun makeApiCallForInAppBitmap(url: String): DownloadedBitmap {
        val request = BitmapDownloadRequest(url)
        return HttpBitmapLoader.getHttpBitmap(
            bitmapOperation = HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP,
            bitmapDownloadRequest = request
        )
    }

}