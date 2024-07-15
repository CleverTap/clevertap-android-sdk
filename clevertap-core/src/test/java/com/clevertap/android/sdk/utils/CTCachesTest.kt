package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import com.clevertap.android.sdk.inapp.images.memory.Memory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class CTCachesTest {
    @After
    fun tearDown() {
        CTCaches.clear() // Clear the singleton instance after each test
    }

    @Test
    fun `instance returns the same instance on multiple calls`() {
        val mockImageMemory = mockk<Memory<Bitmap>>()
        val mockGifMemory = mockk<Memory<ByteArray>>()
        val mockFileMemory = mockk<Memory<ByteArray>>()

        val instance1 = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)
        val instance2 = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)
        assertSame(instance1, instance2)
    }

    @Test
    fun `clear resets the singleton instance`() {
        val mockImageMemory = mockk<Memory<Bitmap>>()
        val mockGifMemory = mockk<Memory<ByteArray>>()
        val mockFileMemory = mockk<Memory<ByteArray>>()

        val instance1 = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)
        CTCaches.clear()
        val instance2 = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)

        assertNotSame(instance1, instance2)
    }

    @Test
    fun `cache access methods return correct caches`() {
        val mockImageMemory = mockk<Memory<Bitmap>>()
        val mockGifMemory = mockk<Memory<ByteArray>>()
        val mockFileMemory = mockk<Memory<ByteArray>>()
        val mockImageLruCache = mockk<LruCache<Pair<Bitmap, File>>>()
        val mockGifLruCache = mockk<LruCache<Pair<ByteArray, File>>>()
        val mockFileLruCache = mockk<LruCache<Pair<ByteArray, File>>>()
        val mockImageFileCache = mockk<FileCache>()
        val mockGifFileCache = mockk<FileCache>()
        val mockFileFileCache = mockk<FileCache>()

        every { mockImageMemory.createInMemory() } returns mockImageLruCache
        every { mockGifMemory.createInMemory() } returns mockGifLruCache
        every { mockFileMemory.createInMemory() } returns mockFileLruCache
        every { mockImageMemory.createDiskMemory() } returns mockImageFileCache
        every { mockGifMemory.createDiskMemory() } returns mockGifFileCache
        every { mockFileMemory.createDiskMemory() } returns mockFileFileCache

        val ctCaches = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)

        assertEquals(mockImageLruCache, ctCaches.imageCache())
        assertEquals(mockGifLruCache, ctCaches.gifCache())
        assertEquals(mockFileLruCache, ctCaches.fileLruCache())
        assertEquals(mockImageFileCache, ctCaches.imageCacheDisk())
        assertEquals(mockGifFileCache, ctCaches.gifCacheDisk())
        assertEquals(mockFileFileCache, ctCaches.fileCacheDisk())
    }
}