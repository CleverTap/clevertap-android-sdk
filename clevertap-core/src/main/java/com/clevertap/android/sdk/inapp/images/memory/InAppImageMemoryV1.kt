package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max
/**
 * A memory management implementation for in-app images, providing both in-memory and disk-based storage.
 *
 * This class utilizes [InMemoryLruCache] for efficient in-memory caching of bitmaps and [DiskMemory] for persistent
 * storage of images on disk. It manages the creation and configuration of these storages based on the provided [MemoryConfig].
 *
 * @param config The configuration object specifying parameters for memory management.
 * @param logger An optional logger for debugging and tracking purposes.
 */
class InAppImageMemoryV1(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<Bitmap> {

    private var imageInMemory: InMemoryLruCache<Pair<Bitmap, File>>? = null
    private var imageDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

    /**
     * Creates and returns an in-memory LRU cache for storing bitmaps.
     *
     * The cache is lazily initialized and configured with a maximum size based on the [MemoryConfig].
     *
     * @return The [InMemoryLruCache] cache instance.
     */
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
    /**
     * Creates and returns a disk-based storage mechanism for persisting bitmaps.
     *
     * The disk storage is lazily initialized and configured based on the [MemoryConfig].
     ** @return [DiskMemory] The disk-based storage instance.
     */
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
    /**
     * Calculates and returns the size of the in-memory cache in kilobytes.
     *
     * The size is determined based on the [MemoryConfig], choosing the larger value between the optimistic size
     * and the minimum in-memory size.
     *
     * @return The size of the in-memory cache in kilobytes.
     */
    override fun inMemorySize(): Int {
        val selected = max(config.optimistic, config.minInMemorySizeKB).toInt()

        logger?.verbose("Image cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }
    /**
     * Clears the in-memory cache, releasing all held bitmaps.
     */
    override fun freeInMemory() {
        imageInMemory?.empty()
        imageInMemory = null
    }
}