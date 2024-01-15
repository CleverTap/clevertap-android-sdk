package com.clevertap.android.sdk.inapp.images.preload

internal data class InAppImagePreloadConfig(
    val parallelDownloads: Int,
) {
    companion object {
        private const val DEFAULT_PARALLEL_DOWNLOAD = 4

        fun default() : InAppImagePreloadConfig = InAppImagePreloadConfig(
                parallelDownloads = DEFAULT_PARALLEL_DOWNLOAD
        )
    }
}