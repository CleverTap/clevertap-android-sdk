package com.clevertap.android.pushtemplates

import android.graphics.Bitmap
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
