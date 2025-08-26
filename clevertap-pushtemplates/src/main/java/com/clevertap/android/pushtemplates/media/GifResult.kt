package com.clevertap.android.pushtemplates.media

import android.graphics.Bitmap

data class GifResult(
    val frames: List<Bitmap>?,
    val duration: Int
) {
    companion object {
        private const val INVALID_DURATION = -1

        fun failure(): GifResult {
            return GifResult(null, INVALID_DURATION)
        }
    }
}