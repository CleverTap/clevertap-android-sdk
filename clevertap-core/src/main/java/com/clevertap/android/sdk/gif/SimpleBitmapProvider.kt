package com.clevertap.android.sdk.gif

import android.graphics.Bitmap

internal open class SimpleBitmapProvider : GifDecoder.BitmapProvider {

    override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }

    override fun obtainByteArray(size: Int): ByteArray {
        return ByteArray(size)
    }

    override fun obtainIntArray(size: Int): IntArray {
        return IntArray(size)
    }

    override fun release(bytes: ByteArray) {
        // no-op
    }

    override fun release(bitmap: Bitmap) {
        bitmap.recycle()
    }

    override fun release(array: IntArray) {
        // no-op
    }
}
