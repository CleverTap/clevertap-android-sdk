package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
import kotlin.math.max

class InAppGifMemoryV1(
    internal val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var gifInMemory: InMemoryLruCache<Pair<ByteArray, File>>? = null
    private var gifDiskMemory: DiskMemory? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

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

    override fun inMemorySize(): Int {
        val selected = max(
            a = config.optimistic,
            b = config.minInMemorySizeKB
        ).toInt()

        logger?.verbose(" Gif cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minInMemorySizeKB}, selected = $selected")

        return selected
    }

    override fun freeInMemory() {
        gifInMemory?.empty()
        gifInMemory = null
    }
}