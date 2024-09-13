package com.clevertap.android.sdk.inapp.images

import android.graphics.BitmapFactory
import java.io.File

fun File?.hasValidBitmap() : Boolean {
    if (this == null || this.exists().not()) {
        return false
    }
    val options = BitmapFactory.Options().also {

    }
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(this.path, options)
    return options.outWidth != -1 && options.outHeight != -1
}

fun String?.isNotNullAndEmpty() : Boolean = isNullOrEmpty().not()