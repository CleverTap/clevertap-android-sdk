package com.clevertap.android.pushtemplates

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.getHttpBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status
import com.clevertap.android.sdk.network.DownloadedBitmapFactory

internal class TemplateRepository(val context: Context, val config: CleverTapInstanceConfig?) {

    companion object {
        private const val BYTES_DOWNLOAD_TIMEOUT_MS = 5000L
    }

    internal fun getBytes(url: String): DownloadedBitmap {
        if (url.isBlank()) {
            Logger.v("Cannot download GIF: URL is empty")
            return DownloadedBitmapFactory.nullBitmapWithStatus(Status.NO_IMAGE)
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
            Logger.v("Cannot download Bitmap: URL is empty")
            return DownloadedBitmapFactory.nullBitmapWithStatus(Status.NO_IMAGE)
        }

        val request = BitmapDownloadRequest(
            url,
            false,
            context,
            null,
        )

        return getHttpBitmap(
            HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
            request
        )
    }
}