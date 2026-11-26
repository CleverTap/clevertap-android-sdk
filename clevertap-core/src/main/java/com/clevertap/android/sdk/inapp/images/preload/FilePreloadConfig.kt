package com.clevertap.android.sdk.inapp.images.preload

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class FilePreloadConfig(
    val parallelDownloads: Int,
) {
    companion object {
        private const val DEFAULT_PARALLEL_DOWNLOAD = 4

        fun default() : FilePreloadConfig = FilePreloadConfig(
                parallelDownloads = DEFAULT_PARALLEL_DOWNLOAD
        )
    }
}