package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max

class InAppImageMemoryV1(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<Bitmap> {

    private var imageInMemory: InMemoryLruCache<Pair<Bitmap, File>>? = null
    private var imageDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

    override fun createInMemory(): InMemoryLruCache<Pair<Bitmap, File>> {
        if (imageInMemory == null) {
            synchronized(inMemoryLock) {
                if (imageInMemory == null) {
                    imageInMemory = InMemoryLruCache(maxSize = inMemorySize())
                }
            }
        }
        return imageInMemory!!
    }

    override fun createDiskMemory(): DiskMemory {
        if (imageDiskMemory == null) {
            synchronized(diskMemoryLock) {
                if (imageDiskMemory == null) {
                    imageDiskMemory = DiskMemory(
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