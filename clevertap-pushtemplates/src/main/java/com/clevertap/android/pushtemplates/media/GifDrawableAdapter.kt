package com.clevertap.android.pushtemplates.media

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.pushtemplates.PTHttpBitmapLoader
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import pl.droidsonroids.gif.GifDrawable

interface GifDrawableAdapter {
    fun create(bytes: ByteArray): GifDrawable
    fun getFrameCount(drawable: GifDrawable): Int
    fun getFrameAt(drawable: GifDrawable, index: Int): Bitmap
    fun getDuration(drawable: GifDrawable): Int
}

class GifDrawableAdapterImpl : GifDrawableAdapter {
    override fun create(bytes: ByteArray) = GifDrawable(bytes)

    override fun getFrameCount(drawable: GifDrawable) = drawable.numberOfFrames

    override fun getFrameAt(drawable: GifDrawable, index: Int): Bitmap =
        drawable.seekToFrameAndGet(index)

    override fun getDuration(drawable: GifDrawable) = drawable.duration
}

internal class TemplateRepository(val context: Context, val config: CleverTapInstanceConfig?) {

    companion object {
        private const val BYTES_DOWNLOAD_TIMEOUT_MS = 5000L
    }

    internal fun getBytes(url: String): DownloadedBitmap {
        if (url.isBlank()) {
            Logger.v("Cannot download GIF: URL is empty")
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
            Logger.v("Cannot download Bitmap: URL is empty")
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