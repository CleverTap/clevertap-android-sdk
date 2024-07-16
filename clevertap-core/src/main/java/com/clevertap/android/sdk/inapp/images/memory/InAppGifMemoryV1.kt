package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max
/**
 * A memory management implementation for in-app GIF images, providing both in-memory and disk-based storage.
 *
 * This class utilizes [InMemoryLruCache] for efficient in-memory caching of GIF images (represented as byte arrays)
 * and [DiskMemory] for persistent storage of GIFs on disk. It manages the creation and configuration of these storages
 * based on the provided [MemoryConfig].
 *
 * @param config The configuration object specifying parameters for memory management.
 * @param logger An optional logger for debugging and tracking purposes.
 */
class InAppGifMemoryV1(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var gifInMemory: InMemoryLruCache<Pair<ByteArray, File>>? = null
    private var gifDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()
    /**
     * Creates and returns an in-memory LRU cache for storing GIF images as byte arrays.
     *
     * The cache is lazily initialized and configured with a maximum size based on the [MemoryConfig].
     *
     * @return The [InMemoryLruCache] cache instance.
     */
    override fun createInMemory(): InMemoryLruCache<Pair<ByteArray, File>> {
        if (gifInMemory == null) {
            synchronized(inMemoryLock) {
                if (gifInMemory == null) {
                    gifInMemory = InMemoryLruCache(maxSize = inMemorySize())
                }
            }
        }
        return gifInMemory!!
    }

    /**
     * Creates and returns a disk-based storage mechanism for persisting GIF images.
     *
     * The disk storage is lazily initialized and configured based on the [MemoryConfig].
     *
     * @return [DiskMemory] The disk-based storage instance.
     */
    override fun createDiskMemory(): DiskMemory {
        if (gifDiskMemory == null) {
            synchronized(diskMemoryLock) {
                if (gifDiskMemory == null) {
                    gifDiskMemory = DiskMemory(
                        directory = config.diskDirectory,
                        maxFileSizeKb = config.maxDiskSizeKB.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return gifDiskMemory!!
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
        val selected = max(
            a = config.optimistic,
            b = config.minInMemorySizeKB
        ).toInt()

        logger?.verbose(" Gif cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }
    /**
     * Clears the in-memory cache, releasing all held GIF images.
     */
    override fun freeInMemory() {
        gifInMemory?.empty()
        gifInMemory = null
    }
}