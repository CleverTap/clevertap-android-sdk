package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MemoryCreatorTest {

    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockDiskDirectory = mockk<File>()

    @Test
    fun `createInAppGifMemoryV1 creates InAppGifMemoryV1 instance`() {
        val expectedGifCacheMinKb: Long = 5 * 1024
        val expectedImageSizeMaxDisk: Long = 5 * 1024

        val gifMemory = MemoryCreator.createInAppGifMemoryV1(mockDiskDirectory, mockLogger)
        assertTrue(gifMemory is InAppGifMemoryV1)


        gifMemory as InAppGifMemoryV1
        // Verify that the created memory object have correct config values
        assertEquals(expectedGifCacheMinKb, gifMemory.config.minInMemorySizeKB)
        assertEquals(expectedImageSizeMaxDisk, gifMemory.config.maxDiskSizeKB)
        assertEquals(mockDiskDirectory, gifMemory.config.diskDirectory)
    }

    @Test
    fun `createInAppImageMemoryV1 creates InAppImageMemoryV1 instance`() {
        val expectedImageCacheMinKb: Long = 20 * 1024
        val expectedImageSizeMaxDisk: Long = 5 * 1024

        val imageMemory = MemoryCreator.createInAppImageMemoryV1(mockDiskDirectory, mockLogger)
        assertTrue(imageMemory is InAppImageMemoryV1)

        imageMemory as InAppImageMemoryV1
        // Verify that the created memory object have correct config values
        assertEquals(expectedImageCacheMinKb, imageMemory.config.minInMemorySizeKB)
        assertEquals(expectedImageSizeMaxDisk, imageMemory.config.maxDiskSizeKB)
        assertEquals(mockDiskDirectory, imageMemory.config.diskDirectory)
    }

    @Test
    fun `createFileMemoryV2 creates FileMemoryV2 instance`() {
        val expectedFileCacheMinKb: Long = 15 * 1024
        val expectedFileSizeMaxDisk: Long = 5 * 1024

        val fileMemory = MemoryCreator.createFileMemoryV2(mockDiskDirectory, mockLogger)
        assertTrue(fileMemory is FileMemoryV2)

        fileMemory as FileMemoryV2
        // Verify that the created memory object have correct config values
        assertEquals(expectedFileCacheMinKb, fileMemory.config.minInMemorySizeKB)
        assertEquals(expectedFileSizeMaxDisk, fileMemory.config.maxDiskSizeKB)
        assertEquals(mockDiskDirectory, fileMemory.config.diskDirectory)
    }
}