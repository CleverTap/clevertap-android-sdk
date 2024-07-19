package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import java.io.File
/**
 * A factory class for creating different types of memory objects for storage purposes.
 *
 * This class provides methods for creating [InAppGifMemoryV1], [InAppImageMemoryV1], and [FileMemoryV2] objects.
 */
class MemoryCreator {
    companion object {

        private const val IMAGE_CACHE_MIN_KB: Long = 20 * 1024
        private const val GIF_CACHE_MIN_KB: Long = 5 * 1024
        private const val FILE_CACHE_MIN_KB: Long = 15 * 1024
        private const val IMAGE_SIZE_MAX_DISK: Long = 5 * 1024
        private const val FILE_SIZE_MAX_DISK: Long = 5 * 1024

        /**
         * Creates a [InAppGifMemoryV1] object for storing GIF images.
         *
         * @param diskMemoryLocation The directory on disk for storing GIF images.
         * @param logger An optional [ILogger] for logging purposes.
         * @return An instance of [InAppGifMemoryV1].
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
         * Creates an [InAppImageMemoryV1] object for storing bitmap images.
         *
         * @param diskMemoryLocation The directory on disk for storing bitmap images.
         * @param logger An optional [ILogger] for logging purposes.
         * @return An instance of [InAppImageMemoryV1].
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
         * Creates a [FileMemoryV2] object for storing all types of file data.
         *
         * @param diskMemoryLocation The directory on disk for storing all types of files.
         * @param logger An optional [ILogger] for logging purposes.
         * @return An instance of [FileMemoryV2].
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