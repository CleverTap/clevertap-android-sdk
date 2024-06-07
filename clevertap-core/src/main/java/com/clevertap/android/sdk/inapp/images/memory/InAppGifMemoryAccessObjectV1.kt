package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BITMAP
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BYTEARRAY
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_FILE
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

class InAppGifMemoryAccessObjectV1(private val ctCaches: CTCaches): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.get(key)
    }

    override fun fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType): Any? {
        val pair = fetchInMemory(key)
        return pair?.let {
            when(transformTo)
            {
                MEMORY_DATA_TRANSFORM_TO_BITMAP -> bytesToBitmap(it.first)
                MEMORY_DATA_TRANSFORM_TO_BYTEARRAY -> it.first
                MEMORY_DATA_TRANSFORM_TO_FILE -> it.second
                else -> null
            }
        }
    }

    override fun fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType): Any? {
        val file = fetchDiskMemory(key)
        return file?.let {
            when(transformTo)
            {
                MEMORY_DATA_TRANSFORM_TO_BITMAP -> fileToBitmap(it)
                MEMORY_DATA_TRANSFORM_TO_BYTEARRAY -> fileToBytes(it)
                MEMORY_DATA_TRANSFORM_TO_FILE -> file
                else -> null
            }
        }
    }

    override fun fetchDiskMemory(key: String): File? {
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
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.add(key, data)
    }
}