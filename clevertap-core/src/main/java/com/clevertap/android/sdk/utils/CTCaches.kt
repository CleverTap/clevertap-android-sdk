package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import java.io.File
import kotlin.math.max

/**
 * We have 2 caches in CT, image cache and a gif cache with different size configs
 */
// todo locking should be individual cache based
class CTCaches private constructor(
    private val config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
    private val logger: ILogger? = null
) {

    companion object {
        private var ctCaches: CTCaches? = null

        fun instance(
            config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
            logger: ILogger?
        ) : CTCaches {
            synchronized(this) {
                if (ctCaches == null) {
                    ctCaches = CTCaches(config = config, logger = logger)
                }
                return ctCaches!!
            }
        }
        fun clear() {
            synchronized(this) {
                ctCaches = null
            }
        }
    }

    private var imageCache: LruCache<Bitmap>? = null
    private var gifCache: LruCache<ByteArray>? = null

    private var imageFileCache: FileCache? = null
    private var gifFileCache: FileCache? = null

    fun imageCache(): LruCache<Bitmap> {
        synchronized(this) {
            if (imageCache == null) {
                imageCache = LruCache(maxSize = imageCacheSize())
            }
            return imageCache!!
        }
    }

    fun gifCache(): LruCache<ByteArray> {
        synchronized(this) {
            if (gifCache == null) {
                gifCache = LruCache(maxSize = gifCacheSize())
            }
            return gifCache!!
        }
    }

    fun imageCacheDisk(dir: File): FileCache {
        synchronized(this) {
            if (imageFileCache == null) {
                imageFileCache = FileCache(
                    directory = dir,
                    maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                    logger = logger
                )
            }
            return imageFileCache!!
        }
    }

    fun gifCacheDisk(dir: File): FileCache {
        synchronized(this) {
            if (gifFileCache == null) {
                gifFileCache = FileCache(
                    directory = dir,
                    maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                    logger = logger
                )
            }
            return gifFileCache!!
        }
    }

    fun imageCacheSize(): Int {
        val selected = max(config.optimistic, config.minImageCacheKb).toInt()

        logger?.verbose("Image cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minImageCacheKb}, selected = $selected")

        return selected
    }

    fun gifCacheSize(): Int {
        val selected = max(config.optimistic, config.minGifCacheKb).toInt()

        logger?.verbose(" Gif cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minGifCacheKb}, selected = $selected")

        return selected
    }

    fun freeMemory() {
        synchronized(this) {
            imageCache?.empty()
            imageCache = null
            gifCache?.empty()
            gifCache = null
        }
    }

}

data class CTCachesConfig(
    val minImageCacheKb: Long,
    val minGifCacheKb: Long,
    val optimistic: Long,
    val maxImageSizeDiskKb: Long
) {
    companion object {
        const val IMAGE_CACHE_MIN_KB : Long = 20 * 1024
        const val GIF_CACHE_MIN_KB : Long = 5 * 1024
        const val IMAGE_SIZE_MAX_DISK : Long = 5 * 1024

        val DEFAULT_CONFIG = CTCachesConfig(
            minImageCacheKb = IMAGE_CACHE_MIN_KB,
            minGifCacheKb = GIF_CACHE_MIN_KB,
            optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
            maxImageSizeDiskKb = IMAGE_SIZE_MAX_DISK
        )
    }
}