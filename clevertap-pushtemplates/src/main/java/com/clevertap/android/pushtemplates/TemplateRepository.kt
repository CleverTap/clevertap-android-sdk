package com.clevertap.android.pushtemplates

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.network.DownloadedBitmap

class TemplateRepository(val context: Context, val config: CleverTapInstanceConfig?) {

    companion object {
        private const val GIF_DOWNLOAD_TIMEOUT_MS = 5000L
    }

    fun getGifBytes(url: String): ByteArray? {
        if (url.isBlank()) {
            Logger.v("Cannot download GIF: URL is null or empty")
            return null
        }

        val request = BitmapDownloadRequest(
            url,
            false,
            context,
            config,
            GIF_DOWNLOAD_TIMEOUT_MS
        )

        val downloadedBitmap = PTHttpBitmapLoader.getHttpBitmap(
            PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
            request
        )

        return if (downloadedBitmap.status == DownloadedBitmap.Status.SUCCESS) {
            downloadedBitmap.bytes
        } else {
            Logger.v("Network call for bitmap download failed with URL: $url, HTTP status: ${downloadedBitmap.status}")
            null
        }
    }
}