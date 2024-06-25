package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
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
        fun createInAppGifMemoryV1(diskMemoryLocation: File, logger: ILogger?): Memory<ByteArray> {
            val defaultGifConfig = MemoryConfig(
                minInMemorySizeKB = GIF_CACHE_MIN_KB,
                optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
                maxDiskSizeKB = IMAGE_SIZE_MAX_DISK,
                diskDirectory = diskMemoryLocation
            )
            return InAppGifMemoryV1(config = defaultGifConfig, logger = logger)
        }

        /**
         * Creates an ImageMemory object.
         * @param diskMemoryLocation The location on disk for memory storage.
         * @param logger The logger for logging purposes.
         * @return An instance of ImageMemory.
         */
        fun createInAppImageMemoryV1(diskMemoryLocation: File, logger: ILogger?): Memory<Bitmap> {
            val defaultImageConfig = MemoryConfig(
                minInMemorySizeKB = IMAGE_CACHE_MIN_KB,
                optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
                maxDiskSizeKB = IMAGE_SIZE_MAX_DISK,
                diskDirectory = diskMemoryLocation
            )
            return InAppImageMemoryV1(config = defaultImageConfig, logger = logger)
        }

        /**
         * Creates a FileMemory object.
         * @param diskMemoryLocation The location on disk for memory storage.
         * @param logger The logger for logging purposes.
         * @return An instance of FileMemory.
         */
        fun createFileMemoryV2(diskMemoryLocation: File, logger: ILogger?): Memory<ByteArray> {
            val defaultFileConfig = MemoryConfig(
                minInMemorySizeKB = FILE_CACHE_MIN_KB,
                optimistic = Runtime.getRuntime().maxMemory() / (1024 * 32),
                maxDiskSizeKB = FILE_SIZE_MAX_DISK,
                diskDirectory = diskMemoryLocation
            )
            return FileMemoryV2(config = defaultFileConfig, logger = logger)
        }
    }
}