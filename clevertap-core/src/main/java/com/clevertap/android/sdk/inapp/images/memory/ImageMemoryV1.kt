package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import java.io.File
import kotlin.math.max

class ImageMemoryV1(
    private val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<Bitmap> {

    private var imageInMemory: LruCache<Pair<Bitmap, File>>? = null
    private var imageDiskMemory: FileCache? = null
    private val lock1 = Any()
    private val lock2 = Any()

    override fun createInMemory(): LruCache<Pair<Bitmap, File>> {
        if (imageInMemory == null) {
            synchronized(lock1) {
                if (imageInMemory == null) {
                    imageInMemory = LruCache(maxSize = inMemorySize())
                }
            }
        }
        return imageInMemory!!
    }

    override fun createDiskMemory(): FileCache {
        if (imageDiskMemory == null) {
            synchronized(lock2) {
                if (imageDiskMemory == null) {
                    imageDiskMemory = FileCache(
                        directory = config.diskDirectory,
                        maxFileSizeKb = config.maxDiskSizeKB.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return imageDiskMemory!!
    }

    override fun inMemorySize(): Int {
        val selected = max(config.optimistic, config.minInMemorySizeKB).toInt()

        logger?.verbose("Image cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }

    override fun freeInMemory() {
        imageInMemory?.empty()
        imageInMemory = null
    }
}