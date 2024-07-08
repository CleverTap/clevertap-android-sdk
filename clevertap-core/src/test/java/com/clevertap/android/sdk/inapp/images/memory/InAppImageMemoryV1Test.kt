package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
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

class InAppImageMemoryV1Test {

    private lateinit var inAppImageMemoryV1: InAppImageMemoryV1
    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockDiskDirectory = mockk<File>()

    @Before
    fun setup() {
        inAppImageMemoryV1 = InAppImageMemoryV1(
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
        val inMemoryCache = inAppImageMemoryV1.createInMemory()
        assertNotNull(inMemoryCache)
    }

    @Test
    fun `createInMemory returns existing in-memory cache if already created`() {
        val inMemoryCache1 = inAppImageMemoryV1.createInMemory()
        val inMemoryCache2 = inAppImageMemoryV1.createInMemory()
        assertSame(inMemoryCache1, inMemoryCache2)
    }

    @Test
    fun `createDiskMemory creates and returns disk cache`() {
        val diskCache = inAppImageMemoryV1.createDiskMemory()
        assertNotNull(diskCache)
    }

    @Test
    fun `createDiskMemory returns existing disk cache if already created`() {
        val diskCache1 = inAppImageMemoryV1.createDiskMemory()
        val diskCache2 = inAppImageMemoryV1.createDiskMemory()
        assertSame(diskCache1, diskCache2)
    }

    @Test
    fun `inMemorySize returns the larger of optimistic and minInMemorySizeKB`() {
        assertEquals(1024, inAppImageMemoryV1.inMemorySize())
    }

    @Test
    fun `freeInMemory clears in-memory cache`() {
        // Populate the in-memory cache
        val testKey = "test_key"
        val testData = Pair(mockk<Bitmap>(), mockk<File>())
        inAppImageMemoryV1.createInMemory().add(testKey, testData)

        // Call freeInMemory
        inAppImageMemoryV1.freeInMemory()

        // Verify that the cache is now empty
        assertNull(inAppImageMemoryV1.createInMemory().get(testKey))
        assertTrue(inAppImageMemoryV1.createInMemory().isEmpty())
    }
}