package com.clevertap.android.pushtemplates.media

import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface GifDrawableAdapter {
    fun create(bytes: ByteArray): GifDrawable
    fun getFrameCount(drawable: GifDrawable): Int
    fun getFrameAt(drawable: GifDrawable, index: Int): Bitmap
    fun getDuration(drawable: GifDrawable): Int
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class GifDrawableAdapterImpl : GifDrawableAdapter {
    override fun create(bytes: ByteArray) = GifDrawable(bytes)

    override fun getFrameCount(drawable: GifDrawable) = drawable.numberOfFrames

    override fun getFrameAt(drawable: GifDrawable, index: Int): Bitmap =
        drawable.seekToFrameAndGet(index)

    override fun getDuration(drawable: GifDrawable) = drawable.duration
}
