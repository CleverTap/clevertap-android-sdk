package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max

class FileMemoryV2(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var fileInMemory: InMemoryLruCache<Pair<ByteArray, File>>? = null
    private var fileDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

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

    override fun inMemorySize(): Int {
        val selected = max(config.optimistic, config.minInMemorySizeKB).toInt()

        logger?.verbose(" File cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }

    override fun freeInMemory() {
        fileInMemory?.empty()
        fileInMemory = null
    }
}