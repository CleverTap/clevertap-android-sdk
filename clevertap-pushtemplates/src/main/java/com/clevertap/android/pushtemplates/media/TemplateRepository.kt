package com.clevertap.android.pushtemplates.media

import android.content.Context
import com.clevertap.android.pushtemplates.PTHttpBitmapLoader
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory

internal class TemplateRepository(val context: Context, val config: CleverTapInstanceConfig?) {

    companion object {
        private const val BYTES_DOWNLOAD_TIMEOUT_MS = 5000L
    }

    internal fun getBytes(url: String): DownloadedBitmap {
        if (url.isBlank()) {
            PTLog.verbose("Cannot download GIF: URL is empty")
            return DownloadedBitmapFactory.nullBitmapWithStatus(DownloadedBitmap.Status.NO_IMAGE)
        }

        val request = BitmapDownloadRequest(
            url,
            false,
            context,
            config,
            BYTES_DOWNLOAD_TIMEOUT_MS
        )

        return PTHttpBitmapLoader.getHttpBitmap(
            PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
            request
        )
    }

    internal fun getBitmap(url: String): DownloadedBitmap {
        if (url.isBlank()) {
            PTLog.verbose("Cannot download Bitmap: URL is empty")
            return DownloadedBitmapFactory.nullBitmapWithStatus(DownloadedBitmap.Status.NO_IMAGE)
        }

        val request = BitmapDownloadRequest(
            url,
            false,
            context,
            null,
        )

        return HttpBitmapLoader.getHttpBitmap(
            HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
            request
        )
    }
}