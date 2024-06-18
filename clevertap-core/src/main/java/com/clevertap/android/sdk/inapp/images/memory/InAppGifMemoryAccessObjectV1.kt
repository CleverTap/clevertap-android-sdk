package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class InAppGifMemoryAccessObjectV1(private val ctCaches: CTCaches,private val logger: ILogger?): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.get(key)
    }

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

    override fun fetchDiskMemory(key: String): File? {
        logger?.verbose(TAG_FILE_DOWNLOAD,"GIF In-Memory cache miss for $key data")
        val gifDiskMemory = ctCaches.gifCacheDisk()
        return gifDiskMemory.get(key)
    }

    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val gifDiskMemory = ctCaches.gifCacheDisk()
        return gifDiskMemory.addAndReturnFileInstance(key, data)
    }

    override fun removeDiskMemory(key: String): Boolean {
        val gifDiskMemory = ctCaches.gifCacheDisk()
        return gifDiskMemory.remove(key)
    }

    override fun removeInMemory(key: String): Pair<ByteArray, File>? {
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.remove(key)
    }

    override fun saveInMemory(key: String, data: Pair<ByteArray, File>): Boolean {
        logger?.verbose(TAG_FILE_DOWNLOAD,"Saving $key data in GIF in-memory")
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.add(key, data)
    }
}