package com.clevertap.android.pushtemplates

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.sdk.network.DownloadedBitmap
import kotlin.system.measureTimeMillis

internal class TemplateMediaManager(
    private val templateRepository: TemplateRepository,
    private val gifDecoder: GifDecoderImpl = GifDecoderImpl())
{
    fun getGifFrames(gifUrl: String?, maxFrames: Int): GifResult {
        if (gifUrl.isNullOrBlank() || !gifUrl.startsWith("https") || !gifUrl.lowercase().endsWith(".gif")) {
            PTLog.verbose("Invalid GIF URL: $gifUrl")
            return GifResult.failure()
        }
        val downloadedBitmap = templateRepository.getBytes(gifUrl)

        val rawBytes = if (downloadedBitmap.status == DownloadedBitmap.Status.SUCCESS) {
            downloadedBitmap.bytes
        } else {
            PTLog.verbose("Network call for GIF failed with URL: $gifUrl, HTTP status: ${downloadedBitmap.status}")
            null
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

        var downloadedBitmap: DownloadedBitmap
        val downloadTime = measureTimeMillis {
            downloadedBitmap = templateRepository.getBitmap(imageUrl)
        }

        PTLog.verbose("Fetched IMAGE $imageUrl in $downloadTime ms")

        return when (downloadedBitmap.status) {
            DownloadedBitmap.Status.SUCCESS -> downloadedBitmap.bitmap
            else -> {
                PTLog.verbose("Bitmap download failed. URL: $imageUrl, Status: ${downloadedBitmap.status}")
                null
            }
        }
    }
}