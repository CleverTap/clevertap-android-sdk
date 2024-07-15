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

class InAppGifMemoryAccessObjectV1Test {

    private lateinit var inAppGifMemoryAccessObjectV1: InAppGifMemoryAccessObjectV1
    private val mockCTCaches = mockk<CTCaches>()
    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockMemoryCache = mockk<InMemoryLruCache<Pair<ByteArray, File>>>()
    private val mockDiskCache = mockk<DiskMemory>()

    private val key = "test_key"
    private val mockByteArray = byteArrayOf(1, 2, 3)
    private val mockFile = mockk<File>()
    private val mockBitmap = mockk<Bitmap>()
    private val mockData = Pair(mockByteArray, mockFile)

    @Before
    fun setup() {
        every { mockCTCaches.gifCache() } returns mockMemoryCache
        every { mockCTCaches.gifCacheDisk() } returns mockDiskCache
        inAppGifMemoryAccessObjectV1 = InAppGifMemoryAccessObjectV1(mockCTCaches, mockLogger)

        mockkStatic("com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObjectKt")
        mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
    }

    @Test
    fun `fetchInMemory returns data from memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData
        assertEquals(mockData, inAppGifMemoryAccessObjectV1.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemory returns null when data not in memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemoryAndTransform transforms data from memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData

        every { bytesToBitmap(mockByteArray) } returns mockBitmap
        assertEquals(
            mockBitmap,
            inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToBitmap)
        )

        assertEquals(
            mockByteArray,
            inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToByteArray)
        )
        assertEquals(mockFile, inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToFile))
    }

    @Test
    fun `fetchInMemoryAndTransform returns null when data not in memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToBitmap))
    }

    @Test
    fun `fetchInMemoryAndTransform returns null when transformation fails`() {
        every { mockMemoryCache.get(key) } returns mockData
        every { bytesToBitmap(mockByteArray) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform(key, ToBitmap))

    }

    @Test
    fun `fetchDiskMemoryAndTransform transforms data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        every { fileToBytes(mockFile) } returns mockByteArray
        every { mockMemoryCache.add(key, mockData) } returns true

        every { fileToBitmap(mockFile) } returns mockBitmap
        assertEquals(
            mockBitmap,
            inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToBitmap)
        )

        assertEquals(
            mockByteArray,
            inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToByteArray)
        )
        assertEquals(
            mockFile,
            inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToFile)
        )
        verify { mockMemoryCache.add(key, mockData) }
    }

    @Test
    fun `fetchDiskMemoryAndTransform returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToBitmap))
    }

    @Test
    fun `fetchDiskMemoryAndTransform returns null when transformation fails`(){
        every { mockDiskCache.get(key) } returns mockFile
        every { fileToBytes(mockFile) } returns mockByteArray
        every {mockMemoryCache.add(key, mockData) } returns true
        every { fileToBitmap(mockFile) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform(key, ToBitmap))
    }

    @Test
    fun `fetchDiskMemory returns data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        assertEquals(mockFile, inAppGifMemoryAccessObjectV1.fetchDiskMemory(key))
    }

    @Test
    fun `fetchDiskMemory returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.fetchDiskMemory(key))
    }

    @Test
    fun `removeDiskMemory removes data from disk cache`() {
        every { mockDiskCache.remove(key) } returns true
        assertTrue(inAppGifMemoryAccessObjectV1.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    @Test
    fun `removeDiskMemory returns false when data not in disk cache`() {
        every { mockDiskCache.remove(key) } returns false
        assertFalse(inAppGifMemoryAccessObjectV1.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    @Test
    fun `removeInMemory removes data from memory cache`() {
        every { mockMemoryCache.remove(key) } returns mockData
        assertEquals(mockData, inAppGifMemoryAccessObjectV1.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }

    @Test
    fun `removeInMemory returns null when data not in memory cache`() {
        every { mockMemoryCache.remove(key) } returns null
        assertNull(inAppGifMemoryAccessObjectV1.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }

    @Test
    fun `saveDiskMemory saves data to disk cache`() {
        every { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) } returns mockFile
        assertEquals(mockFile, inAppGifMemoryAccessObjectV1.saveDiskMemory(key, mockByteArray))
        verify { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) }
    }

    @Test
    fun `saveInMemory saves data to memory cache`() {
        every { mockMemoryCache.add(key, mockData) } returns true
        assertTrue(inAppGifMemoryAccessObjectV1.saveInMemory(key, mockData))
        verify { mockMemoryCache.add(key, mockData) }
    }
}