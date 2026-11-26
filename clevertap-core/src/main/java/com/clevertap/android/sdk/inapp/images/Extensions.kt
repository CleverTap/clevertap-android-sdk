package com.clevertap.android.sdk.inapp.images

import android.graphics.BitmapFactory
import java.io.File
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun File?.hasValidBitmap() : Boolean {
    if (this == null || this.exists().not()) {
        return false
    }
    val options = BitmapFactory.Options().also {

    }
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(this.path, options)
    return options.outWidth != -1 && options.outHeight != -1
}
