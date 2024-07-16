package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File
/**
 * An implementation of [MemoryAccessObject] for managing images in memory and on disk.
 *
 * This class provides methods for fetching, saving, and removing images from both in-memory and disk storage.
 * It also supports transforming fetched data into different formats like [Bitmap], [ByteArray], and [File].
 *
 * @param ctCaches An instance of [CTCaches] providing access to the caching mechanisms.
 * @param logger An optional [ILogger] for debugging and tracking purposes.
 */
internal class InAppImageMemoryAccessObjectV1(private val ctCaches: CTCaches,private val logger: ILogger?) : MemoryAccessObject<Bitmap> {
    /**
     * Fetches an image from in-memory cache by key.
     *
     * @param key The key to search for.
     * @return A [Pair] containing the image [Bitmap] and [File] object if found, or null otherwise.
     */
    override fun fetchInMemory(key: String): Pair<Bitmap, File>? {
        val imageInMemory = ctCaches.imageInMemory()
        return imageInMemory.get(key)
    }
    /**
     * Fetches an image from in-memory cache by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the image is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val pair = fetchInMemory(key)
        return pair?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in image in-memory")
            when(transformTo)
            {
                ToBitmap -> it.first as? A
                ToByteArray -> bitmapToBytes(it.first as Bitmap) as? A
                ToFile -> it.second as? A
            }
        }
    }
    /**
     * Fetches an image from disk memory by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the image is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val file = fetchDiskMemory(key)
        return file?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in image disk memory")
            val bitmap = fileToBitmap(it)
            if (bitmap != null) {
                saveInMemory(key, Pair(bitmap, it))
            }
            when(transformTo)
            {
                ToBitmap -> bitmap as? A
                ToByteArray -> fileToBytes(it) as? A
                ToFile -> it as? A
            }
        }
    }
    /**
     * Fetches an image file from disk memory by key.
     *
     * @param key The key to search for.
     * @return The [File] object if found, or null otherwise.
     */
    override fun fetchDiskMemory(key: String): File? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"IMAGE In-Memory cache miss for $key data")
        val imageDiskMemory = ctCaches.imageDiskMemory()
        return imageDiskMemory.get(key)
    }
    /**
     * Saves an image to in-memory cache.
     *
     * @param key The key to save the image under.
     * @param data A [Pair] containing the image [Bitmap] and [File] object.
     * @return True if the image was saved successfully, false otherwise.
     */
    override fun saveInMemory(key: String, data: Pair<Bitmap, File>): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"Saving $key data in IMAGE in-memory")
        val imageInMemory = ctCaches.imageInMemory()
        return imageInMemory.add(key, data)
    }
    /**
     * Saves an image to disk memory.
     *
     * @param key The key to save the image under.
     * @param data The image data as a [ByteArray].
     * @return The saved [File] object.
     */
    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val imageDiskMemory = ctCaches.imageDiskMemory()
        return imageDiskMemory.addAndReturnFileInstance(key, data)
    }
    /**
     * Removes an image from disk memory by key.
     *
     * @param key The key to remove the image for.
     * @return True if the image was removed successfully, false otherwise.
     */
    override fun removeDiskMemory(key: String): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from IMAGE disk-memory")
        val imageDiskMemory = ctCaches.imageDiskMemory()
        return imageDiskMemory.remove(key)
    }
    /**
     * Removes an image from in-memory cache by key.
     *
     * @param key The key to remove the image for.
     * @return The removed image data ([Bitmap] and [File] object) if found, or null otherwise.
     */
    override fun removeInMemory(key: String): Pair<Bitmap, File>? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from IMAGE in-memory")
        val imageInMemory = ctCaches.imageInMemory()
        return imageInMemory.remove(key)
    }
}