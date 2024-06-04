package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryV2
import com.clevertap.android.sdk.inapp.images.memory.GifMemoryV1
import com.clevertap.android.sdk.inapp.images.memory.ImageMemoryV1
import com.clevertap.android.sdk.inapp.images.memory.Memory
import java.io.File
import kotlin.math.max

/**
 * We have 2 caches in CT, image cache and a gif cache with different size configs
 */
class CTCaches private constructor(
   /* private val config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
    private val logger: ILogger? = null,*/
    private val imageMemory: Memory<Bitmap>,
    private val gifMemory: Memory<ByteArray>,
    private val fileMemory: Memory<ByteArray>
) {

    companion object {
        private var ctCaches: CTCaches? = null

        private val lock1 = Any()
        private val lock2 = Any()
        private val lock3 = Any()
        private val lock4 = Any()
        private val lock5 = Any()
        private val lock6 = Any()

        fun instance(
           /* config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
            logger: ILogger?,*/
            imageMemory: Memory<Bitmap>,
            gifMemory: Memory<ByteArray>,
            fileMemory: Memory<ByteArray>
        ): CTCaches {
            if (ctCaches == null) {
                synchronized(this) {
                    if (ctCaches == null) {
                        ctCaches = CTCaches(/*config, logger,*/ imageMemory, gifMemory, fileMemory)
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

    private var imageCache: LruCache<Pair<Bitmap,File>>? = null
    private var gifCache: LruCache<Pair<ByteArray,File>>? = null
    private var fileLruCache: LruCache<Pair<ByteArray,File>>? = null

    private var imageFileCache: FileCache? = null
    private var gifFileCache: FileCache? = null
    private var fileCacheDisk: FileCache? = null

    fun imageCache(): LruCache<Pair<Bitmap,File>> {
        return imageMemory.createInMemory()
        /*if (imageCache == null) {
            synchronized(lock1) {
                if (imageCache == null) {
                    imageCache = LruCache(maxSize = imageCacheSize())
                }
            }
        }
        return imageCache!!*/
    }

    fun gifCache(): LruCache<Pair<ByteArray,File>> {
        return gifMemory.createInMemory()
        /*if (gifCache == null) {
            synchronized(lock2) {
                if (gifCache == null) {
                    gifCache = LruCache(maxSize = gifCacheSize())
                }
            }
        }
        return gifCache!!*/
    }

    fun fileLruCache(): LruCache<Pair<ByteArray,File>> {
        return fileMemory.createInMemory()
       /* if (fileLruCache == null) {
            synchronized(lock5) {
                if (fileLruCache == null) {
                    fileLruCache = LruCache(maxSize = fileLruCacheSize())
                }
            }
        }
        return fileLruCache!!*/
    }

    fun imageCacheDisk(/*dir: File*/): FileCache {
        return imageMemory.createDiskMemory()
        /*if (imageFileCache == null) {
            synchronized(lock3) {
                if (imageFileCache == null) {
                    imageFileCache = FileCache(
                        directory = dir,
                        maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return imageFileCache!!*/
    }

    fun gifCacheDisk(/*dir: File*/): FileCache {
        return gifMemory.createDiskMemory()
        /*if (gifFileCache == null) {
            synchronized(lock4) {
                if (gifFileCache == null) {
                    gifFileCache = FileCache(
                        directory = dir,
                        maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return gifFileCache!!*/
    }

    fun fileCacheDisk(/*dir: File*/): FileCache {
        return fileMemory.createDiskMemory()
        /*if (fileCacheDisk == null) {
            synchronized(lock6) {
                if (fileCacheDisk == null) {
                    fileCacheDisk = FileCache(
                        directory = dir,
                        maxFileSizeKb = config.maxFileSizeDiskKB.toInt(),
                        logger = logger
                    )
                }
            }
        }
        return fileCacheDisk!!*/
    }

    /*fun imageCacheSize(): Int {
        val selected = max(config.optimistic, config.minImageCacheKb).toInt()

        logger?.verbose("Image cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minImageCacheKb}, selected = $selected")

        return selected
    }

    fun gifCacheSize(): Int {
        val selected = max(config.optimistic, config.minGifCacheKb).toInt()

        logger?.verbose(" Gif cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minGifCacheKb}, selected = $selected")

        return selected
    }

    fun fileLruCacheSize(): Int {
        val selected = max(config.optimistic, config.minFileSizeCacheKB).toInt()

        logger?.verbose(" File cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minFileSizeCacheKB}, selected = $selected")

        return selected
    }

    fun freeMemory() {
        synchronized(this) {
            imageCache?.empty()
            imageCache = null
            gifCache?.empty()
            gifCache = null
        }
    }*/

}

/*
data class CTCachesConfig(
    val minImageCacheKb: Long,
    val minGifCacheKb: Long,
    val minFileSizeCacheKB: Long,
    val optimistic: Long,
    val maxImageSizeDiskKb: Long,
    val maxFileSizeDiskKB: Long
) {
    companion object {
        const val IMAGE_CACHE_MIN_KB : Long = 20 * 1024
        const val GIF_CACHE_MIN_KB : Long = 5 * 1024
        const val FILE_CACHE_MIN_KB : Long = 15 * 1024
        const val IMAGE_SIZE_MAX_DISK : Long = 5 * 1024
        const val FILE_SIZE_MAX_DISK : Long = 15 * 1024

        val DEFAULT_CONFIG = CTCachesConfig(
            minImageCacheKb = IMAGE_CACHE_MIN_KB,
            minGifCacheKb = GIF_CACHE_MIN_KB,
            minFileSizeCacheKB = FILE_CACHE_MIN_KB,
            optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
            maxImageSizeDiskKb = IMAGE_SIZE_MAX_DISK,
            maxFileSizeDiskKB = FILE_SIZE_MAX_DISK
        )
    }
}*/
