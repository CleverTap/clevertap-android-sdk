package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import com.clevertap.android.sdk.inapp.images.memory.Memory
import java.io.File

/**
 * We have 2 caches in CT, image cache and a gif cache with different size configs
 */
class CTCaches private constructor(
    private val inAppImageMemoryV1: Memory<Bitmap>,
    private val inAppGifMemoryV1: Memory<ByteArray>,
    private val fileMemory: Memory<ByteArray>
) {

    companion object {

        private var ctCaches: CTCaches? = null
        fun instance(
            inAppImageMemoryV1: Memory<Bitmap>,
            inAppGifMemoryV1: Memory<ByteArray>,
            fileMemory: Memory<ByteArray>
        ): CTCaches {
            if (ctCaches == null) {
                synchronized(this) {
                    if (ctCaches == null) {
                        ctCaches = CTCaches(
                            inAppImageMemoryV1 = inAppImageMemoryV1,
                            inAppGifMemoryV1 = inAppGifMemoryV1,
                            fileMemory = fileMemory
                        )
                    }
                }
            }
            return ctCaches!!
        }

        fun clear() {
            synchronized(this) {
                ctCaches = null
            }
        }
    }

    fun imageCache(): LruCache<Pair<Bitmap, File>> {
        return inAppImageMemoryV1.createInMemory()
    }

    fun gifCache(): LruCache<Pair<ByteArray, File>> {
        return inAppGifMemoryV1.createInMemory()
    }

    fun fileLruCache(): LruCache<Pair<ByteArray, File>> {
        return fileMemory.createInMemory()
    }

    fun imageCacheDisk(): FileCache {
        return inAppImageMemoryV1.createDiskMemory()
    }

    fun gifCacheDisk(): FileCache {
        return inAppGifMemoryV1.createDiskMemory()
    }

    fun fileCacheDisk(): FileCache {
        return fileMemory.createDiskMemory()
    }
}