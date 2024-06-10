package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import java.io.File
import kotlin.math.max

// CTCaches - SDK Class - all features -> inapp, templates

// 2 features:
// Inapps -> InAppResorceProvider, InAppsRepo
// Templates -> FileResorceProvider, FileResourcesRepoImpl
// m modueles


// Step 3
// Inapps -> FileResorceProvider, FileResourcesRepoImpl

// Step 4
// Inapps -> getInappImage() -> inappresprov.getInappImage -> if null -> FileResorceProvider.getInappImage


// FileResourceImpl
// FileResourceProvicer
// Gif, Image, File cache

class InAppGifMemoryV1(
    private val config: MemoryConfig,
    private val logger: ILogger? = null
) : Memory<ByteArray> {

    private var gifInMemory: LruCache<Pair<ByteArray, File>>? = null
    private var gifDiskMemory: FileCache? = null
    private val inMemoryLock = Any()
    private val diskMemoryLock = Any()

    override fun createInMemory(): LruCache<Pair<ByteArray, File>> {
        if (gifInMemory == null) {
            synchronized(inMemoryLock) {
                if (gifInMemory == null) {
                    gifInMemory = LruCache(maxSize = inMemorySize())
                }
            }
        }
        return gifInMemory!!
    }

    override fun createDiskMemory(): FileCache {
        if (gifDiskMemory == null) {
            synchronized(diskMemoryLock) {
                if (gifDiskMemory == null) {
                    gifDiskMemory = FileCache(
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