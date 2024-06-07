package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BITMAP
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_BYTEARRAY
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.MEMORY_DATA_TRANSFORM_TO_FILE
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

class InAppImageMemoryAccessObjectV1(private val ctCaches: CTCaches) : MemoryAccessObject<Bitmap> {

    override fun fetchInMemory(key: String): Pair<Bitmap, File>? {
        val imageInMemory = ctCaches.imageCache()
        return imageInMemory.get(key)
    }

    override fun fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType): Any? {
        val pair = fetchInMemory(key)
        return pair?.let {
            when(transformTo)
            {
                MEMORY_DATA_TRANSFORM_TO_BITMAP -> it.first
                MEMORY_DATA_TRANSFORM_TO_BYTEARRAY -> bitmapToBytes(it.first)
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
        val imageDiskMemory = ctCaches.imageCacheDisk()
        return imageDiskMemory.get(key)
    }

    override fun saveInMemory(key: String, data: Pair<Bitmap, File>): Boolean {
        val imageInMemory = ctCaches.imageCache()
        return imageInMemory.add(key, data)
    }

    override fun saveDiskMemory(key: String, data: ByteArray): File {
        val imageDiskMemory = ctCaches.imageCacheDisk()
        return imageDiskMemory.addAndReturnFileInstance(key, data)
    }

    override fun removeDiskMemory(key: String): Boolean {
        val imageDiskMemory = ctCaches.imageCacheDisk()
        return imageDiskMemory.remove(key)
    }

    override fun removeInMemory(key: String): Pair<Bitmap, File>? {
        val imageInMemory = ctCaches.imageCache()
        return imageInMemory.remove(key)
    }
}