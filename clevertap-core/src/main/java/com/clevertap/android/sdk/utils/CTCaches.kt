package com.clevertap.android.sdk.utils

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import kotlin.math.max

/**
 * We have 2 caches in CT, image cache and a gif cache with different size configs
 */
// todo locking should be individual cache based
class CTCaches private constructor(
    val config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
    val logger: ILogger? = null
) {

    companion object {
        private var ctCaches: CTCaches? = null

        private const val IMAGE_DIRECTORY_NAME = "CleverTap.Images."
        private const val GIF_DIRECTORY_NAME = "CleverTap.Gif."

        fun instance(
            logger: ILogger?
        ) : CTCaches {
            synchronized(this) {
                if (ctCaches == null) {
                    ctCaches = CTCaches(logger = logger)
                }
                return ctCaches!!
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

    fun imageCacheDisk(context: Context): FileCache {
        synchronized(this) {
            if (imageFileCache == null) {
                imageFileCache = FileCache(
                    directory = context.getDir("directoryName", Context.MODE_PRIVATE),
                    maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                    logger = logger
                )
            }
            return imageFileCache!!
        }
    }

    fun gifCacheDisk(context: Context): FileCache {
        synchronized(this) {
            if (gifFileCache == null) {
                gifFileCache = FileCache(
                    directory = context.getDir("directoryName", Context.MODE_PRIVATE),
                    maxFileSizeKb = config.maxImageSizeDiskKb.toInt(),
                    logger = logger
                )
            }
            return gifFileCache!!
        }
    }

    private fun imageCacheSize(): Int {
        val selected = max(config.optimistic, config.minImageCacheKb).toInt()

        logger?.verbose("Image cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minImageCacheKb}, selected = $selected")

        return selected
    }

    private fun gifCacheSize(): Int {
        val selected = max(config.optimistic, config.minImageCacheKb).toInt()

        logger?.verbose(" Gif cache:: max-mem/1024 = ${config.optimistic}, minCacheSize = ${config.minImageCacheKb}, selected = $selected")

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
        val DEFAULT_CONFIG = CTCachesConfig(
            minImageCacheKb = 20 * 1024,
            minGifCacheKb = 5 * 1024,
            optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
            maxImageSizeDiskKb = 5 * 1024
        )
    }
}