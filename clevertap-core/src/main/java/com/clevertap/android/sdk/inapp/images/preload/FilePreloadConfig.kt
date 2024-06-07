package com.clevertap.android.sdk.inapp.images.preload

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