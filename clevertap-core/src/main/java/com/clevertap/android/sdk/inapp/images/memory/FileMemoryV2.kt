package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import java.io.File
import kotlin.math.max

class FileMemoryV2(
    private val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var fileInMemory: LruCache<Pair<ByteArray, File>>? = null
    private var fileDiskMemory: FileCache? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

    override fun createInMemory(): LruCache<Pair<ByteArray, File>> {
        if (fileInMemory == null) {
            synchronized(inMemoryLock) {
                if (fileInMemory == null) {
                    fileInMemory = LruCache(maxSize = inMemorySize())
                }
            }
        }
        return fileInMemory!!
    }

    override fun createDiskMemory(): FileCache {
        if (fileDiskMemory == null) {
            synchronized(diskMemoryLock) {
                if (fileDiskMemory == null) {
                    fileDiskMemory = FileCache(
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