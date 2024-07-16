package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File
/**
 * An implementation of [MemoryAccessObject] for managing GIF images in memory and on disk.
 *
 * This class provides methods for fetching, saving, and removing GIF images from both in-memory and disk storage.
 * It also supports transforming fetched data into different formats like [Bitmap], [ByteArray], and [File].
 *
 *@param ctCaches An instance of [CTCaches] providing access to the caching mechanisms.
 * @param logger An optional [ILogger] for debugging and tracking purposes.
 */
internal class InAppGifMemoryAccessObjectV1(private val ctCaches: CTCaches,private val logger: ILogger?): MemoryAccessObject<ByteArray> {
    /**
     * Fetches a GIF image from in-memory cache by key.
     *
     * @param key The key to search for.
     * @return A [Pair] containing the GIF's [ByteArray] and [File] object if found, or null otherwise.
     */
    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val gifInMemory = ctCaches.gifInMemory()
        return gifInMemory.get(key)
    }

    /**
     * Fetches a GIF image from in-memory cache by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the GIF is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val pair = fetchInMemory(key)
        return pair?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in GIF in-memory")
            when(transformTo)
            {
                ToBitmap -> bytesToBitmap(it.first) as? A
                ToByteArray -> it.first as? A
                ToFile -> it.second as? A
            }
        }
    }

    /**
     * Fetches a GIF image from disk memory by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the GIF is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val file = fetchDiskMemory(key)
        return file?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in GIF disk memory")
            val bytes = fileToBytes(it)
            if (bytes != null) {
                saveInMemory(key, Pair(bytes, it))
            }
            when(transformTo)
            {
                ToBitmap -> fileToBitmap(it) as? A
                ToByteArray -> bytes as? A
                ToFile -> it as? A
            }
        }
    }
    /**
     * Fetches a GIF image file from disk memory by key.
     *
     * @param key The key to search for.
     * @return The [File] object if found, or null otherwise.
     */
    override fun fetchDiskMemory(key: String): File? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"GIF In-Memory cache miss for $key data")
        val gifDiskMemory = ctCaches.gifDiskMemory()
        return gifDiskMemory.get(key)
    }
    /**
     * Saves a GIF image to disk memory.
     *
     * @param key The key to save the GIF under.
     * @param data The GIF data as a [ByteArray].
     * @return The saved [File] object.
     */
    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val gifDiskMemory = ctCaches.gifDiskMemory()
        return gifDiskMemory.addAndReturnFileInstance(key, data)
    }
    /**
     * Removes a GIF image from disk memory by key.
     *
     * @param key The key to remove the GIF for.
     * @return True if the GIF was removed successfully, false otherwise.
     */
    override fun removeDiskMemory(key: String): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from GIF disk-memory")
        val gifDiskMemory = ctCaches.gifDiskMemory()
        return gifDiskMemory.remove(key)
    }
    /**
     * Removes a GIF image from in-memory cache by key.
     *
     * @param key The key to remove the GIF for.
     * @return The removed GIF data ([ByteArray] and [File] object) if found, or null otherwise.
     */
    override fun removeInMemory(key: String): Pair<ByteArray, File>? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from GIF in-memory")
        val gifInMemory = ctCaches.gifInMemory()
        return gifInMemory.remove(key)
    }
    /**
     * Saves a GIF image to in-memory cache.
     *
     * @param key The key to save the GIF under.
     * @param data A [Pair] containing the GIF's [ByteArray] and [File] object.
     * @return True if the GIF was saved successfully, false otherwise.
     */
    override fun saveInMemory(key: String, data: Pair<ByteArray, File>): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"Saving $key data in GIF in-memory")
        val gifInMemory = ctCaches.gifInMemory()
        return gifInMemory.add(key, data)
    }
}