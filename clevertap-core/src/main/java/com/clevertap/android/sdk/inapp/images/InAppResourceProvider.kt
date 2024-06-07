package com.clevertap.android.sdk.inapp.images

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.GifMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.ImageMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryCreator
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BITMAP
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BYTEARRAY
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_FILE
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.utils.CTCaches
import java.io.ByteArrayOutputStream
import java.io.File

internal class InAppResourceProvider(
    private val images: File,
    private val gifs: File,
    private val allFileTypesDir: File,
    private val logger: ILogger? = null,
    private val inAppRemoteSource: InAppImageFetchApiContract = InAppImageFetchApi()
) {

    private var ctCaches: CTCaches = CTCaches.instance(
        imageMemory = MemoryCreator.createImageMemory(images, logger),
        gifMemory = MemoryCreator.createGifMemory(gifs, logger),
        fileMemory = MemoryCreator.createFileMemory(allFileTypesDir, logger)
    )

    constructor(
        context: Context,
        logger: ILogger? = null
    ) : this(
        images = context.getDir(IMAGE_DIRECTORY_NAME, Context.MODE_PRIVATE),
        gifs = context.getDir(GIF_DIRECTORY_NAME, Context.MODE_PRIVATE),
        allFileTypesDir = context.getDir(ALL_FILE_TYPES_DIRECTORY_NAME, Context.MODE_PRIVATE),
        logger = logger
    )

    companion object {

        private const val IMAGE_DIRECTORY_NAME = "CleverTap.Images."
        private const val GIF_DIRECTORY_NAME = "CleverTap.Gif."
        private const val ALL_FILE_TYPES_DIRECTORY_NAME = "CleverTap.Files."
    }

    fun saveImage(cacheKey: String, bitmap: Bitmap, bytes: ByteArray) {
        val imageMAO = ImageMemoryAccessObject(ctCaches)
        val savedFile = imageMAO.saveDiskMemory(cacheKey, bytes)
        imageMAO.saveInMemory(cacheKey, Pair(bitmap, savedFile))
    }

    fun saveGif(cacheKey: String, bytes: ByteArray) {
        val gifMAO = GifMemoryAccessObject(ctCaches)
        val savedFile = gifMAO.saveDiskMemory(cacheKey, bytes)
        gifMAO.saveInMemory(cacheKey, Pair(bytes, savedFile))
    }

    fun saveFile(cacheKey: String, bytes: ByteArray) {
        val fileMAO = FileMemoryAccessObject(ctCaches)
        val savedFile = fileMAO.saveDiskMemory(cacheKey, bytes)
        fileMAO.saveInMemory(cacheKey, Pair(bytes, savedFile))
    }

    fun isImageCached(url: String): Boolean {
        return isFileCached(url)
    }

    fun isGifCached(url: String): Boolean {
        return isFileCached(url)
    }

    fun isFileCached(url: String): Boolean {
        val memoryAccessObjectList = listOf<MemoryAccessObject<*>>(
            FileMemoryAccessObject(ctCaches), ImageMemoryAccessObject(ctCaches),
            GifMemoryAccessObject(ctCaches)
        )

        // Try in memory
        memoryAccessObjectList.forEach {
            val pair = it.fetchInMemory(url)
            if (pair != null) {
                return true
            }
        }

        // Try disk
        memoryAccessObjectList.forEach {
            val file = it.fetchDiskMemory(url)
            if (file != null) {
                return true
            }
        }
        return false
    }

    fun cachedImage(cacheKey: String?): Bitmap? {

        if (cacheKey == null) {
            logger?.verbose("Bitmap for null key requested")
            return null
        }

        val memoryAccessObjectList = listOf<MemoryAccessObject<*>>(
            ImageMemoryAccessObject(ctCaches), FileMemoryAccessObject(ctCaches),
            GifMemoryAccessObject(ctCaches)
        )
        // Try in memory
        memoryAccessObjectList.forEach {
            val bitmap = it.fetchInMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BITMAP)
            if (bitmap is Bitmap) {
                return bitmap
            }
        }

        // Try disk
        memoryAccessObjectList.forEach {
            val bitmap = it.fetchDiskMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BITMAP)
            if (bitmap is Bitmap) {
                return bitmap
            }
        }

        return null
    }

    fun cachedGif(cacheKey: String?): ByteArray? {
        if (cacheKey == null) {
            logger?.verbose("GIF for null key requested")
            return null
        }
        val memoryAccessObjectList = listOf<MemoryAccessObject<*>>(
            GifMemoryAccessObject(ctCaches), FileMemoryAccessObject(ctCaches),
            ImageMemoryAccessObject(ctCaches)
        )
        // Try in memory
        memoryAccessObjectList.forEach {
            val bytes = it.fetchInMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BYTEARRAY)
            if (bytes is ByteArray) {
                return bytes
            }
        }

        // Try disk
        memoryAccessObjectList.forEach {
            val bytes = it.fetchDiskMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BYTEARRAY)
            if (bytes is ByteArray) {
                return bytes
            }
        }

        return null
    }

    fun cachedFileInBytes(cacheKey: String?): ByteArray? {
        if (cacheKey == null) {
            logger?.verbose("File for null key requested")
            return null
        }
        val memoryAccessObjectList = listOf<MemoryAccessObject<*>>(
            FileMemoryAccessObject(ctCaches),
            GifMemoryAccessObject(ctCaches),
            ImageMemoryAccessObject(ctCaches)
        )
        // Try in memory
        memoryAccessObjectList.forEach {
            val bytes = it.fetchInMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BYTEARRAY)
            if (bytes is ByteArray) {
                return bytes
            }
        }

        // Try disk
        memoryAccessObjectList.forEach {
            val bytes = it.fetchDiskMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_BYTEARRAY)
            if (bytes is ByteArray) {
                return bytes
            }
        }

        return null
    }

    fun cachedFilePath(cacheKey: String?): String? {
        return cachedFileInstance(cacheKey)?.absolutePath
    }

    fun cachedFileInstance(cacheKey: String?): File? {
        if (cacheKey == null) {
            logger?.verbose("File for null key requested")
            return null
        }
        val memoryAccessObjectList = listOf<MemoryAccessObject<*>>(
            FileMemoryAccessObject(ctCaches),
            GifMemoryAccessObject(ctCaches),
            ImageMemoryAccessObject(ctCaches)
        )
        // Try in memory
        memoryAccessObjectList.forEach {
            val file = it.fetchInMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_FILE)
            if (file is File) {
                return file
            }
        }

        // Try disk
        memoryAccessObjectList.forEach {
            val file = it.fetchDiskMemoryAndTransform(cacheKey, MEMORY_DATA_TRANSFORM_TO_FILE)
            if (file is File) {
                return file
            }
        }
        return null
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

    fun fetchInAppGif(url: String): ByteArray? {
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

    fun fetchFile(url: String): ByteArray? {
        val cachedFile = cachedFileInBytes(url)

        if (cachedFile != null) {
            logger?.verbose("Returning requested $url file from cache with size ${cachedFile.size}")
            return cachedFile
        }

        val downloadedFile = inAppRemoteSource.makeApiCallForInAppBitmap(url = url)

        return when (downloadedFile.status) {

            DownloadedBitmap.Status.SUCCESS -> {
                saveFile(cacheKey = url, bytes = downloadedFile.bytes!!)
                logger?.verbose("Returning requested $url file with network, saved in cache")
                downloadedFile.bytes
            }

            else -> {
                logger?.verbose("There was a problem fetching data for file, status:${downloadedFile.status}")
                null
            }
        }
    }

    fun deleteImage(cacheKey: String) {
        val imageMAO = ImageMemoryAccessObject(ctCaches)
        val pair = imageMAO.removeInMemory(cacheKey)
        if (pair != null) {
            logger?.verbose("successfully removed $cacheKey from memory cache")
        }

        val b = imageMAO.removeDiskMemory(cacheKey)
        if (b) {
            logger?.verbose("successfully removed $cacheKey from file cache")
        }
    }

    fun deleteGif(cacheKey: String) {

        val gifMAO = GifMemoryAccessObject(ctCaches)
        val bytes = gifMAO.removeInMemory(cacheKey)
        if (bytes != null) {
            logger?.verbose("successfully removed gif $cacheKey from memory cache")
        }
        val b = gifMAO.removeDiskMemory(cacheKey)
        if (b) {
            logger?.verbose("successfully removed gif $cacheKey from file cache")
        }
    }

    fun deleteFile(cacheKey: String) {
        val fileMAO = FileMemoryAccessObject(ctCaches)
        val bytes = fileMAO.removeInMemory(cacheKey)

        if (bytes != null) {
            logger?.verbose("successfully removed file $cacheKey from memory cache")
        }
        val b = fileMAO.removeDiskMemory(cacheKey)

        if (b) {
            logger?.verbose("successfully removed file $cacheKey from file disk cache")
        }
    }
}