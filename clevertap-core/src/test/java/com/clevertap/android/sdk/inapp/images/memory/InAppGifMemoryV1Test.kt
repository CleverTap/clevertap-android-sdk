package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.ILogger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class InAppGifMemoryV1Test {

    private lateinit var inAppGifMemoryV1: InAppGifMemoryV1
    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockDiskDirectory = mockk<File>()

    @Before
    fun setup() {
        inAppGifMemoryV1 = InAppGifMemoryV1(
            config = MemoryConfig(
                minInMemorySizeKB = 512,
                optimistic = 1024,
                maxDiskSizeKB = 2048,
                diskDirectory = mockDiskDirectory
            ),
            logger = mockLogger
        )
    }

    @Test
    fun `createInMemory creates and returns in-memory cache`() {
        val inMemoryCache = inAppGifMemoryV1.createInMemory()
        assertNotNull(inMemoryCache)
    }

    @Test
    fun `createInMemory returns existing in-memory cache if already created`() {
        val inMemoryCache1 = inAppGifMemoryV1.createInMemory()
        val inMemoryCache2 = inAppGifMemoryV1.createInMemory()
        assertSame(inMemoryCache1, inMemoryCache2)
    }

    @Test
    fun `createDiskMemory creates and returns disk cache`() {
        val diskCache = inAppGifMemoryV1.createDiskMemory()
        assertNotNull(diskCache)
    }

    @Test
    fun `createDiskMemory returns existing diskcache if already created`() {
        val diskCache1 = inAppGifMemoryV1.createDiskMemory()
        val diskCache2 = inAppGifMemoryV1.createDiskMemory()
        assertSame(diskCache1, diskCache2)
    }

    @Test
    fun `inMemorySize returns the larger of optimistic and minInMemorySizeKB`() {
        assertEquals(1024, inAppGifMemoryV1.inMemorySize())
    }

    @Test
    fun `freeInMemory clears in-memory cache`() {
        // Populate the in-memory cache
        val testKey = "test_key"
        val testData = Pair(byteArrayOf(1, 2, 3), mockk<File>())
        inAppGifMemoryV1.createInMemory().add(testKey, testData)

        // Call freeInMemory
        inAppGifMemoryV1.freeInMemory()

        // Verify that the cache is now empty
        assertNull(inAppGifMemoryV1.createInMemory().get(testKey))
        assertTrue(inAppGifMemoryV1.createInMemory().isEmpty())
    }
}