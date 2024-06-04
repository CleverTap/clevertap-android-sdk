package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

class FileMemoryAccessObject(private val ctCaches: CTCaches): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val fileInMemory = ctCaches.fileLruCache()
        return fileInMemory.get(key)
    }

    override fun fetchInMemoryAndTransform(key: String, transformTo: Int): Any? {
        val pair = fetchInMemory(key)
        return pair?.let {
            when(transformTo)
            {
                TRANSFORM_TO_BITMAP -> bytesToBitmap(it.first)
                TRANSFORM_TO_BYTEARRAY -> it.first
                TRANSFORM_TO_FILE -> it.second
                else -> null
            }
        }
    }

    override fun fetchDiskMemoryAndTransform(key: String, transformTo: Int): Any? {
        val file = fetchDiskMemory(key)
        return file?.let {
            when(transformTo)
            {
                TRANSFORM_TO_BITMAP -> fileToBitmap(it)
                TRANSFORM_TO_BYTEARRAY -> fileToBytes(it)
                TRANSFORM_TO_FILE -> file
                else -> null
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