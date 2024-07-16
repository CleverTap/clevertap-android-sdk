package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

/**
 * An implementation of [MemoryAccessObject] for managing files in memory and on disk.
 *
 * This class provides methods for fetching, saving, and removing files from both in-memory and disk caches.
 * It also supports transforming fetched data into different formats like [Bitmap], [ByteArray], and [File].
 *
 * @param ctCaches An instance of [CTCaches] providing access to the caching mechanisms.
 * @param logger An optional [ILogger] for debugging and tracking purposes.
 */
internal class FileMemoryAccessObject(private val ctCaches: CTCaches,private val logger: ILogger? = null): MemoryAccessObject<ByteArray> {
    /**
     * Fetches a file from in-memory cache by key.
     *
     * @param key The key to search for.
     * @return A [Pair] containing the file's [ByteArray] and [File] object if found, or null otherwise.
     */
    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val fileInMemory = ctCaches.fileInMemory()
        return fileInMemory.get(key)
    }
    /**
     * Fetches a file from in-memory cache by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the file is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val pair = fetchInMemory(key)
        return pair?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in FILE in-memory")
            when(transformTo)
            {
                ToBitmap -> bytesToBitmap(it.first) as? A
                ToByteArray -> it.first as? A
                ToFile -> it.second as? A
            }
        }
    }
    /**
     * Fetches a file from disk memory by key and transforms it.
     *
     * @param key The key to search for.
     * @param transformTo The transformation identifier ([ToBitmap], [ToByteArray], or [ToFile]).
     * @return The transformed data, or null if the file is not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val file = fetchDiskMemory(key)
        return file?.let {
            logger?.verbose(TAG_FILE_DOWNLOAD,"$key data found in FILE disk memory")
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
     * Fetches a file from disk memory by key.
     *
     * @param key The key to search for.
     * @return The [File] object if found, or null otherwise.
     */
    override fun fetchDiskMemory(key: String): File? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"FILE In-Memory cache miss for $key data")
        val fileDiskMemory = ctCaches.fileDiskMemory()
        return fileDiskMemory.get(key)
    }
    /**
     * Saves a file to disk memory.
     *
     * @param key The key to save the file under.
     *@param data The file data as a byte array.
     * @return The saved [File] object.
     */
    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val fileDiskMemory = ctCaches.fileDiskMemory()
        return fileDiskMemory.addAndReturnFileInstance(key, data)
    }
    /**
     * Removes a file from disk memory by key.
     *
     * @param key The key to remove the file for.
     * @return True if the file was removed successfully, false otherwise.
     */
    override fun removeDiskMemory(key: String): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from FILE disk-memory")
        val fileDiskMemory = ctCaches.fileDiskMemory()
        return fileDiskMemory.remove(key)
    }
    /**
     * Removes a file from in-memory cache by key.
     *
     * @param key The key to remove the file for.
     * @return The removed file data (byte array and file object) if found, or null otherwise.
     */
    override fun removeInMemory(key: String): Pair<ByteArray, File>? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from FILE in-memory")
        val fileInMemory = ctCaches.fileInMemory()
        return fileInMemory.remove(key)
    }
    /**
     * Saves a file to in-memory cache.
     *
     * @param key The key to save the file under.
     * @param data A [Pair] containing the file's [ByteArray] and [File] object.
     * @return True if the file was saved successfully, false otherwise.
     */
    override fun saveInMemory(key: String, data: Pair<ByteArray, File>): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"Saving $key data in FILE in-memory")
        val fileInMemory = ctCaches.fileInMemory()
        return fileInMemory.add(key, data)
    }
}