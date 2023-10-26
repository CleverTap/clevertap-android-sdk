package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import kotlin.math.max

/**
 * We have 2 caches in CT, image cache and a gif cache with different size configs
 */
class CTCaches private constructor(
    val config: CTCachesConfig = CTCachesConfig.DEFAULT_CONFIG,
    val logger: ILogger? = null
) {

    companion object {
        private var ctCaches: CTCaches? = null

        private const val IMAGE_DIRECTORY_NAME = "CleverTap.Images."
        private const val GIF_DIRECTORY_NAME = "CleverTap.Gif."
    }

    private var imageCache: Cache<Bitmap>? = null
    private var gifCache: Cache<ByteArray>? = null

    fun instance(
        logger: Logger
    ) : CTCaches {
        synchronized(this) {
            if (ctCaches == null) {
                ctCaches = CTCaches(logger = logger)
            }
            return ctCaches!!
        }
    }

    fun imageCache(): Cache<Bitmap> {
        synchronized(this) {
            if (imageCache == null) {
                imageCache = Cache(maxSize = imageCacheSize())
            }
            return imageCache!!
        }
    }

    fun gifCache(): Cache<ByteArray> {
        synchronized(this) {
            if (gifCache == null) {
                gifCache = Cache(maxSize = gifCacheSize())
            }
            return gifCache!!
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
    val minImageCacheDiskKb: Long,
    val minGifCacheDiskKb: Long,
    val optimistic: Long
) {
    companion object {
        val DEFAULT_CONFIG = CTCachesConfig(
            minImageCacheKb = 20 * 1024,
            minGifCacheKb = 5 * 1024,
            minImageCacheDiskKb = 5 * 1024 * 1024,
            minGifCacheDiskKb = 5 * 1024 * 1024,
            optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32)
        )
    }
}