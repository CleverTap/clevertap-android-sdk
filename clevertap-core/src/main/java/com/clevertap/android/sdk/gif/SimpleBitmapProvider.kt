package com.clevertap.android.sdk.gif

import android.graphics.Bitmap
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class SimpleBitmapProvider : GifDecoder.BitmapProvider {

    override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap =
        Bitmap.createBitmap(width, height, config)

    override fun obtainByteArray(size: Int): ByteArray = ByteArray(size)

    override fun obtainIntArray(size: Int): IntArray = IntArray(size)

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
