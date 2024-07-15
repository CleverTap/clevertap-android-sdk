package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.utils.CTCaches
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class InAppImageMemoryAccessObjectV1Test {

    private lateinit var inAppImageMemoryAccessObjectV1: InAppImageMemoryAccessObjectV1
    private val mockCTCaches = mockk<CTCaches>()
    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockMemoryCache = mockk<InMemoryLruCache<Pair<Bitmap, File>>>()
    private val mockDiskCache = mockk<DiskMemory>()

    private val key = "test_key"
    private val mockBitmap = mockk<Bitmap>()
    private val mockFile = mockk<File>()
    private val mockByteArray = byteArrayOf(1, 2, 3)
    private val mockData = Pair(mockBitmap, mockFile)

    @Before
    fun setup() {
        every { mockCTCaches.imageCache() } returns mockMemoryCache
        every { mockCTCaches.imageCacheDisk() } returns mockDiskCache
        inAppImageMemoryAccessObjectV1 = InAppImageMemoryAccessObjectV1(mockCTCaches, mockLogger)

        mockkStatic("com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObjectKt")
        mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
    }

    @Test
    fun `fetchInMemory returns data from memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData
        assertEquals(mockData, inAppImageMemoryAccessObjectV1.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemory returns null when data not in memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(inAppImageMemoryAccessObjectV1.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemoryAndTransform transforms data from memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData

        assertEquals(
            mockBitmap,
            inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToBitmap)
        )

        every { bitmapToBytes(mockBitmap) } returns mockByteArray
        assertEquals(
            mockByteArray,
            inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToByteArray)
        )

        assertEquals(
            mockFile,
            inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToFile)
        )
    }

    @Test
    fun `fetchInMemoryAndTransform returns null when data not in memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(
            inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform(
                key,
                ToBitmap
            )
        )
    }

    @Test
    fun `fetchInMemoryAndTransform returns null when transformation fails`() {
        every { mockMemoryCache.get(key) } returns mockData
        every { bitmapToBytes(mockBitmap) } returns null
        assertNull(
            inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform(
                key,
                ToByteArray
            )
        )
    }

    @Test
    fun `fetchDiskMemoryAndTransform transforms data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        every { fileToBitmap(mockFile) } returns mockBitmap
        every { mockMemoryCache.add(key, mockData) } returns true

        assertEquals(
            mockBitmap,
            inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToBitmap)
        )
        verify { mockMemoryCache.add(key, mockData) }

        every { fileToBytes(mockFile) } returns mockByteArray
        assertEquals(
            mockByteArray,
            inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToByteArray)
        )

        assertEquals(
            mockFile,
            inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToFile)
        )
    }

    @Test
    fun `fetchDiskMemoryAndTransform returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(
            inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform(
                key,
                ToBitmap
            )
        )
    }

    @Test
    fun `fetchDiskMemoryAndTransform returns null when transformation fails`() {
        every { mockDiskCache.get(key) } returns mockFile
        every { fileToBitmap(mockFile) } returns null
        assertNull(
            inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform(
                key,
                ToBitmap
            )
        )
    }

    @Test
    fun `fetchDiskMemory returns data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        assertEquals(mockFile, inAppImageMemoryAccessObjectV1.fetchDiskMemory(key))
    }

    @Test
    fun `fetchDiskMemory returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(inAppImageMemoryAccessObjectV1.fetchDiskMemory(key))
    }

    @Test
    fun `removeDiskMemory removes data from disk cache`() {
        every { mockDiskCache.remove(key) } returns true
        assertTrue(inAppImageMemoryAccessObjectV1.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    @Test
    fun `removeDiskMemory returns false when data not in disk cache`() {
        every { mockDiskCache.remove(key) } returns false
        assertFalse(inAppImageMemoryAccessObjectV1.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    @Test
    fun `removeInMemory removes data from memory cache`() {
        every { mockMemoryCache.remove(key) } returns mockData
        assertEquals(mockData, inAppImageMemoryAccessObjectV1.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }

    @Test
    fun `removeInMemory returns null when data not in memory cache`() {
        every { mockMemoryCache.remove(key) } returns null
        assertNull(inAppImageMemoryAccessObjectV1.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }

    @Test
    fun `saveDiskMemory saves data to disk cache`() {
        every { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) } returns mockFile
        assertEquals(
            mockFile,
            inAppImageMemoryAccessObjectV1.saveDiskMemory(key, mockByteArray)
        )
        verify { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) }
    }

    @Test
    fun `saveInMemory saves data to memory cache`() {
        every { mockMemoryCache.add(key, mockData) } returns true
        assertTrue(inAppImageMemoryAccessObjectV1.saveInMemory(key, mockData))
        verify { mockMemoryCache.add(key, mockData) }
    }
}