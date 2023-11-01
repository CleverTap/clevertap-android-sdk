package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppImageFetchApi.makeApiCallForInAppBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches
import java.io.ByteArrayOutputStream

internal class InAppResourceProvider(
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

    fun saveGif(cacheKey: String, bytes: ByteArray) {
        val gifMemoryCache = ctCaches.gifCache()
        gifMemoryCache.add(cacheKey, bytes)

        val gifDiskCache = ctCaches.gifCacheDisk(context = context)
        gifDiskCache.add(cacheKey, bytes)
    }

    fun isCached(url: String) : Boolean {
        val imageMemoryCache = ctCaches.imageCache()

        if (imageMemoryCache.get(url) != null) {
            return true
        }

        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        val file = imageDiskCache.get(url)

        return (file != null)
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

        if (file != null && file.hasValidBitmap()) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        logger?.verbose("cached image not present for url : $cacheKey")
        return null
    }

    fun cachedGif(cacheKey: String): ByteArray? {
        // Try in memory
        val gifMemoryCache = ctCaches.gifCache()
        val gifStream = gifMemoryCache.get(cacheKey)

        if (gifStream != null) {
            return gifStream
        }

        val gifDiskCache = ctCaches.gifCacheDisk(context = context)

        return gifDiskCache.get(cacheKey)?.readBytes()
    }

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    inline fun <reified T> fetchInAppImage(url: String): T? {

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
                saveImage(
                    cacheKey = url,
                    bitmap = downloadedBitmap.bitmap!!,
                    bytes = downloadedBitmap.bytes!!
                )
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

    fun fetchInAppGif(url: String) : ByteArray? {
        val cachedGif = cachedGif(url)

        if (cachedGif != null) {
            logger?.verbose("Returning requested $url gif from cache with size ${cachedGif.size}")
            return cachedGif
        }

        val downloadedGif = makeApiCallForInAppBitmap(url = url)

        return when (downloadedGif.status) {

            DownloadedBitmap.Status.SUCCESS -> {
                saveGif(cacheKey = url, bytes = downloadedGif.bytes!!)
                logger?.verbose("Returning requested $url gif with network, saved in cache")
                downloadedGif.bytes
            }

            else -> {
                logger?.verbose("There was a problem fetching data for bitmap, status:${downloadedGif.status}")
                null
            }
        }

    }

    fun deleteImage(cacheKey: String) {
        val imageMemoryCache = ctCaches.imageCache()
        imageMemoryCache.remove(cacheKey)

        val imageDiskCache = ctCaches.imageCacheDisk(context = context)
        imageDiskCache.remove(cacheKey)
    }

    fun deleteGif(cacheKey: String) {
        val imageMemoryCache = ctCaches.gifCache()
        imageMemoryCache.remove(cacheKey)

        val imageDiskCache = ctCaches.gifCacheDisk(context = context)
        imageDiskCache.remove(cacheKey)
    }
}