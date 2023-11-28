package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches
import java.io.ByteArrayOutputStream
import java.io.File

internal class InAppResourceProvider constructor(
    val images: File,
    val gifs: File,
    val logger: ILogger? = null,
    private val ctCaches: CTCaches = CTCaches.instance(logger = logger),
    private val fileToBitmap : (file: File?) -> Bitmap? = { file ->
        if (file != null && file.hasValidBitmap()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    },
    private val fileToBytes: (file: File?) -> ByteArray? = { file ->
        file?.readBytes()
    },
    private val inAppRemoteSource: InAppImageFetchApiContract = InAppImageFetchApi()
) {
    constructor(
            context: Context,
            logger: ILogger? = null
    ) : this(
            images = context.getDir(IMAGE_DIRECTORY_NAME, Context.MODE_PRIVATE),
            gifs = context.getDir(GIF_DIRECTORY_NAME, Context.MODE_PRIVATE),
            logger = logger,
            ctCaches = CTCaches.instance(logger = logger)
    )
    companion object {
        private const val IMAGE_DIRECTORY_NAME = "CleverTap.Images."
        private const val GIF_DIRECTORY_NAME = "CleverTap.Gif."
    }

    fun saveImage(cacheKey: String, bitmap: Bitmap, bytes: ByteArray) {

        val imageMemoryCache = ctCaches.imageCache()
        imageMemoryCache.add(cacheKey, bitmap)

        val imageDiskCache = ctCaches.imageCacheDisk(dir = images)
        imageDiskCache.add(cacheKey, bytes)
    }

    fun saveGif(cacheKey: String, bytes: ByteArray) {
        val gifMemoryCache = ctCaches.gifCache()
        gifMemoryCache.add(cacheKey, bytes)

        val gifDiskCache = ctCaches.gifCacheDisk(dir = gifs)
        gifDiskCache.add(cacheKey, bytes)
    }

    fun isImageCached(url: String) : Boolean {
        val imageMemoryCache = ctCaches.imageCache()

        if (imageMemoryCache.get(url) != null) {
            return true
        }

        val imageDiskCache = ctCaches.imageCacheDisk(dir = images)
        val file = imageDiskCache.get(url)

        return (file != null)
    }

    fun isGifCached(url: String) : Boolean {
        val gifMemoryCache = ctCaches.gifCache()

        if (gifMemoryCache.get(url) != null) {
            return true
        }

        val gifDiskCache = ctCaches.gifCacheDisk(dir = images)
        val file = gifDiskCache.get(url)

        return (file != null)
    }

    fun cachedImage(cacheKey: String?): Bitmap? {

        if (cacheKey == null) {
            logger?.verbose("Bitmap for null key requested")
            return null
        }

        // Try in memory
        val imageMemoryCache = ctCaches.imageCache()
        val bitmap = imageMemoryCache.get(cacheKey)

        if (bitmap != null) {
            return bitmap
        }

        // Try disk
        val imageDiskCache = ctCaches.imageCacheDisk(dir = images)
        val file = imageDiskCache.get(cacheKey)

        val bitmapFromFile = fileToBitmap(file)
        logger?.verbose("cached image for url : $cacheKey, bitmap : ${bitmapFromFile.hashCode()}")
        return bitmapFromFile
    }

    fun cachedGif(cacheKey: String?): ByteArray? {
        if (cacheKey == null) {
            logger?.verbose("GIF for null key requested")
            return null
        }
        // Try in memory
        val gifMemoryCache = ctCaches.gifCache()
        val gifStream = gifMemoryCache.get(cacheKey)

        if (gifStream != null) {
            return gifStream
        }

        val gifDiskCache = ctCaches.gifCacheDisk(dir = gifs)

        return fileToBytes(gifDiskCache.get(cacheKey))
    }

    fun fetchInAppImage(url: String): Bitmap? {
        return fetchInAppImage(url = url, clazz = Bitmap::class.java)
    }

    /**
     * Function that would fetch and cache bitmap image into Memory and File cache and return it.
     * If image is found in cache, the cached image is returned.
     */
    fun <T> fetchInAppImage(url: String, clazz: Class<T>): T? {

        val cachedImage: Bitmap? = cachedImage(url)

        if (cachedImage != null) {
            if (clazz.isAssignableFrom(Bitmap::class.java)) {
                return cachedImage as? T
            } else if (clazz.isAssignableFrom(ByteArray::class.java)) {
                val stream = ByteArrayOutputStream()
                cachedImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                return byteArray as? T
            }
        }

        val downloadedBitmap = inAppRemoteSource.makeApiCallForInAppBitmap(url = url)

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

        val downloadedGif = inAppRemoteSource.makeApiCallForInAppBitmap(url = url)

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
        val bitmap = imageMemoryCache.remove(cacheKey)

        if (bitmap != null) {
            logger?.verbose("successfully removed $cacheKey from memory cache")
        }

        val imageDiskCache = ctCaches.imageCacheDisk(dir = images)
        val b = imageDiskCache.remove(cacheKey)

        if (b) {
            logger?.verbose("successfully removed $cacheKey from file cache")
        }
    }

    fun deleteGif(cacheKey: String) {
        val imageMemoryCache = ctCaches.gifCache()
        val bytes = imageMemoryCache.remove(cacheKey)

        if (bytes != null) {
            logger?.verbose("successfully removed gif $cacheKey from memory cache")
        }

        val imageDiskCache = ctCaches.gifCacheDisk(dir = gifs)
        val b = imageDiskCache.remove(cacheKey)

        if (b) {
            logger?.verbose("successfully removed gif $cacheKey from file cache")
        }
    }
}