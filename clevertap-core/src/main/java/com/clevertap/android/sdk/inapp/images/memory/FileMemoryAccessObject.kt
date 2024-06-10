package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BITMAP
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BYTEARRAY
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_FILE
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class FileMemoryAccessObject(private val ctCaches: CTCaches): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.get(key)
    }

    override fun fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType): Any? {
        val pair = fetchInMemory(key)
        return pair?.let {
            when(transformTo)
            {
                MEMORY_DATA_TRANSFORM_TO_BITMAP -> bytesToBitmap(it.first)
                MEMORY_DATA_TRANSFORM_TO_BYTEARRAY -> it.first
                MEMORY_DATA_TRANSFORM_TO_FILE -> it.second
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
            }
        }
    }

    override fun fetchDiskMemory(key: String): File? {
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.get(key)
    }

    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.addAndReturnFileInstance(key, data)
    }

    override fun removeDiskMemory(key: String): Boolean {
        val fileDiskMemory = ctCaches.fileCacheDisk()
        return fileDiskMemory.remove(key)
    }

    override fun removeInMemory(key: String): Pair<ByteArray, File>? {
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.remove(key)
    }

    override fun saveInMemory(key: String, data: Pair<ByteArray, File>): Boolean {
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.add(key, data)
    }
}