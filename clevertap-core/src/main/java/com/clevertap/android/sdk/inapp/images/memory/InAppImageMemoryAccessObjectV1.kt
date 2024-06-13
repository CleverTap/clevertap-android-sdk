package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class InAppImageMemoryAccessObjectV1(private val ctCaches: CTCaches) : MemoryAccessObject<Bitmap> {

    override fun fetchInMemory(key: String): Pair<Bitmap, File>? {
        val imageInMemory = ctCaches.imageCache()
        return imageInMemory.get(key)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val pair = fetchInMemory(key)
        return pair?.let {
            when(transformTo)
            {
                ToBitmap -> it.first as? A
                ToByteArray -> bitmapToBytes(it.first as Bitmap) as? A
                ToFile -> it.second as? A
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val file = fetchDiskMemory(key)
        return file?.let {
            when(transformTo)
            {
                ToBitmap -> fileToBitmap(it) as? A
                ToByteArray -> fileToBytes(it) as? A
                ToFile -> it as? A
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