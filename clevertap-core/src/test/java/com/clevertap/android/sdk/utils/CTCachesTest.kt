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
        val mockImageInMemoryLruCache = mockk<InMemoryLruCache<Pair<Bitmap, File>>>()
        val mockGifInMemoryLruCache = mockk<InMemoryLruCache<Pair<ByteArray, File>>>()
        val mockFileInMemoryLruCache = mockk<InMemoryLruCache<Pair<ByteArray, File>>>()
        val mockImageDiskMemory = mockk<DiskMemory>()
        val mockGifDiskMemory = mockk<DiskMemory>()
        val mockFileDiskMemory = mockk<DiskMemory>()

        every { mockImageMemory.createInMemory() } returns mockImageInMemoryLruCache
        every { mockGifMemory.createInMemory() } returns mockGifInMemoryLruCache
        every { mockFileMemory.createInMemory() } returns mockFileInMemoryLruCache
        every { mockImageMemory.createDiskMemory() } returns mockImageDiskMemory
        every { mockGifMemory.createDiskMemory() } returns mockGifDiskMemory
        every { mockFileMemory.createDiskMemory() } returns mockFileDiskMemory

        val ctCaches = CTCaches.instance(mockImageMemory, mockGifMemory, mockFileMemory)

        assertEquals(mockImageInMemoryLruCache, ctCaches.imageInMemory())
        assertEquals(mockGifInMemoryLruCache, ctCaches.gifInMemory())
        assertEquals(mockFileInMemoryLruCache, ctCaches.fileInMemory())
        assertEquals(mockImageDiskMemory, ctCaches.imageDiskMemory())
        assertEquals(mockGifDiskMemory, ctCaches.gifDiskMemory())
        assertEquals(mockFileDiskMemory, ctCaches.fileDiskMemory())
    }
}