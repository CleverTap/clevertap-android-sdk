package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import java.io.File

class MemoryCreator {
    companion object {

        private const val IMAGE_CACHE_MIN_KB: Long = 20 * 1024
        private const val GIF_CACHE_MIN_KB: Long = 5 * 1024
        private const val FILE_CACHE_MIN_KB: Long = 15 * 1024
        private const val IMAGE_SIZE_MAX_DISK: Long = 5 * 1024
        private const val FILE_SIZE_MAX_DISK: Long = 15 * 1024

        /**
         * Creates a GifMemory object.
         * @param diskMemoryLocation The location for disk memory storage.
         * @param logger The logger for logging purposes.
         * @return An instance of GifMemory.
         */
        fun createGifMemory(diskMemoryLocation: File, logger: ILogger?): Memory<ByteArray> {
            val defaultGifConfig = MemoryConfig(
                GIF_CACHE_MIN_KB,
                Runtime.getRuntime().maxMemory() / (1024 * 32),
                IMAGE_SIZE_MAX_DISK,
                diskMemoryLocation
            )
            return GifMemoryV1(defaultGifConfig, logger)
        }

        /**
         * Creates an ImageMemory object.
         * @param diskMemoryLocation The location on disk for memory storage.
         * @param logger The logger for logging purposes.
         * @return An instance of ImageMemory.
         */
        fun createImageMemory(diskMemoryLocation: File, logger: ILogger?): Memory<Bitmap> {
            val defaultImageConfig = MemoryConfig(
                IMAGE_CACHE_MIN_KB,
                Runtime.getRuntime().maxMemory() / (1024 * 32),
                IMAGE_SIZE_MAX_DISK,
                diskMemoryLocation
            )
            return ImageMemoryV1(defaultImageConfig, logger)
        }

        /**
         * Creates a FileMemory object.
         * @param diskMemoryLocation The location on disk for memory storage.
         * @param logger The logger for logging purposes.
         * @return An instance of FileMemory.
         */
        fun createFileMemory(diskMemoryLocation: File, logger: ILogger?): Memory<ByteArray> {
            val defaultFileConfig = MemoryConfig(
                FILE_CACHE_MIN_KB,
                Runtime.getRuntime().maxMemory() / (1024 * 32),
                FILE_SIZE_MAX_DISK,
                diskMemoryLocation
            )
            return FileMemoryV2(defaultFileConfig, logger)
        }
    }
}