package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.utils.CTCaches
import java.io.File

internal class InAppGifMemoryAccessObjectV1(private val ctCaches: CTCaches): MemoryAccessObject<ByteArray> {

    override fun fetchInMemory(key: String): Pair<ByteArray, File>? {
        val gifInMemory = ctCaches.gifCache()
        return gifInMemory.get(key)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A? {
        val pair = fetchInMemory(key)
        return pair?.let {
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
            when(transformTo)
            {
                ToBitmap -> fileToBitmap(it) as? A
                ToByteArray -> fileToBytes(it) as? A
                ToFile -> it as? A
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