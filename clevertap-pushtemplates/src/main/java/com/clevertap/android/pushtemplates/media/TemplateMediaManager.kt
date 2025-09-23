package com.clevertap.android.pushtemplates.media

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import kotlin.system.measureTimeMillis

internal class TemplateMediaManager(
    private val templateRepository: TemplateRepository,
    private val gifDecoder: GifDecoderImpl = GifDecoderImpl()
) {

    // Simple in-memory cache to avoid duplicate downloads of successful results
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private val bytesCache = mutableMapOf<String, ByteArray>()

    fun getGifFrames(gifUrl: String?, maxFrames: Int): GifResult {
        if (gifUrl.isNullOrBlank() || !gifUrl.startsWith("https") || !gifUrl.lowercase()
                .endsWith(".gif")
        ) {
            PTLog.verbose("Invalid GIF URL: $gifUrl")
            return GifResult.failure()
        }

        // Check if already downloaded and successful
        val cachedBytes = bytesCache[gifUrl]
        val rawBytes = if (cachedBytes != null) {
            PTLog.verbose("GIF loaded from cache: $gifUrl")
            cachedBytes
        } else {
            // Download (or re-download if previous attempt failed)
            val downloadedBitmap = templateRepository.getBytes(gifUrl)
            val bytes = if (downloadedBitmap.status == DownloadedBitmap.Status.SUCCESS) {
                downloadedBitmap.bytes
            } else {
                PTLog.verbose("Network call for GIF failed with URL: $gifUrl, HTTP status: ${downloadedBitmap.status}")
                null
            }
            // Only cache successful downloads
            if (bytes != null) {
                bytesCache[gifUrl] = bytes
            }
            bytes
        }

        return if (rawBytes != null) {
            gifDecoder.decode(rawBytes, maxFrames)
        } else {
            GifResult.failure()
        }
    }

    @Throws(NullPointerException::class)
    fun getNotificationBitmap(
        icoPath: String?, fallbackToAppIcon: Boolean,
        context: Context?
    ): Bitmap? {
        if (icoPath.isNullOrEmpty())
            return if (fallbackToAppIcon)
                Utils.getAppIcon(context)
            else null

        return getImageBitmap(icoPath)
            ?: if (fallbackToAppIcon)
                Utils.getAppIcon(context)
            else null
    }

    fun getImageBitmap(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("https")) {
            PTLog.debug("Invalid IMAGE URL: $imageUrl")
            return null
        }

        // Check if already downloaded and successful
        val cachedBitmap = bitmapCache[imageUrl]
        return if (cachedBitmap != null) {
            PTLog.verbose("IMAGE loaded from cache: $imageUrl")
            cachedBitmap
        } else {
            // Download (or re-download if previous attempt failed or bitmap was recycled)
            var downloadedBitmap: DownloadedBitmap
            val downloadTime = measureTimeMillis {
                downloadedBitmap = templateRepository.getBitmap(imageUrl)
            }

            val bitmap = when (downloadedBitmap.status) {
                DownloadedBitmap.Status.SUCCESS -> {
                    PTLog.verbose("Fetched IMAGE $imageUrl in $downloadTime ms")
                    downloadedBitmap.bitmap
                }
                else -> {
                    PTLog.verbose("Bitmap download failed. URL: $imageUrl, Status: ${downloadedBitmap.status}")
                    null
                }
            }

            // Only cache successful downloads
            if (bitmap != null) {
                bitmapCache[imageUrl] = bitmap
            }
            bitmap
        }
    }

    /**
     * Clears both bitmap and bytes caches. Useful for cleanup after template processing.
     */
    fun clearCaches() {
        bitmapCache.clear()
        bytesCache.clear()
        PTLog.verbose("Media caches cleared")
    }
}