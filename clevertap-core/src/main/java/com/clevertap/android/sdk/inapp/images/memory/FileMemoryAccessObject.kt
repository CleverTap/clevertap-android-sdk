package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class FileMemoryAccessObject(private val ctCaches: CTCaches,private val logger: ILogger? = null): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.get(key)
    }

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

    override fun fetchDiskMemory(key: String): File? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"FILE In-Memory cache miss for $key data")
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.get(key)
    }

    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.addAndReturnFileInstance(key, data)
    }

    override fun removeDiskMemory(key: String): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from FILE disk-memory")
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.remove(key)
    }

    override fun removeInMemory(key: String): Pair<ByteArray, File>? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"If present, will remove $key data from FILE in-memory")
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.remove(key)
    }

    override fun saveInMemory(key: String, data: Pair<ByteArray, File>): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"Saving $key data in FILE in-memory")
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.add(key, data)
    }
}