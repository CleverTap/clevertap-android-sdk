package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max
/**
 * A memory management implementation for all type of files, providing both in-memory and disk-based storage.
 *
 * This class utilizes [InMemoryLruCache] for efficient in-memory caching of files (represented as byte arrays)
 * and [DiskMemory] for persistent storage of files on disk. It manages the creation and configuration of these storages
 * based on the provided [MemoryConfig].
 *
 * @param config The configuration object specifying parameters for memory management.
 * @param logger An optional logger for debugging and tracking purposes.
 */
class FileMemoryV2(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var fileInMemory: InMemoryLruCache<Pair<ByteArray, File>>? = null
    private var fileDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()
    /**
     * Creates and returns an in-memory LRU cache for storing all type of files as byte arrays.
     *
     * The cache is lazily initialized and configured with a maximum size based on the [MemoryConfig].
     *
     * @return [InMemoryLruCache] The in-memory LRU cache instance.
     */
    override fun createInMemory(): InMemoryLruCache<Pair<ByteArray, File>> {
        if (fileInMemory == null) {
            synchronized(inMemoryLock) {
                if (fileInMemory == null) {
                    fileInMemory = InMemoryLruCache(maxSize = inMemorySize())
                }
            }
        }
        return fileInMemory!!
    }
    /**
     * Creates and returns a disk-based storage mechanism for persisting all type of files.
     *
     * The disk storage is lazily initialized and configured based on the [MemoryConfig].
     *
     * @return [DiskMemory] The disk-based storage instance.
     */
    override fun createDiskMemory(): DiskMemory {
        if (fileDiskMemory == null) {
            synchronized(diskMemoryLock) {
                if (fileDiskMemory == null) {
                    fileDiskMemory = DiskMemory(
                        directory = config.diskDirectory,
                        maxFileSizeKb = config.maxDiskSizeKB.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return fileDiskMemory!!
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

        logger?.verbose(" File cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }
    /**
     * Clears the in-memory cache, releasing all type of held files.
     */
    override fun freeInMemory() {
        fileInMemory?.empty()
        fileInMemory = null
    }
}