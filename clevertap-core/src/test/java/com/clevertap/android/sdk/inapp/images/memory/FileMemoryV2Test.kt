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

class FileMemoryV2Test {

    private lateinit var fileMemoryV2: FileMemoryV2
    private val mockLogger = mockk<ILogger>(relaxed = true)
    private val mockDiskDirectory = mockk<File>()

    @Before
    fun setup() {
        fileMemoryV2 = FileMemoryV2(
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
        val inMemoryCache = fileMemoryV2.createInMemory()
        assertNotNull(inMemoryCache)
    }

    @Test
    fun `createInMemory returns existing in-memory cache if already created`() {
        val inMemoryCache1 = fileMemoryV2.createInMemory()
        val inMemoryCache2 = fileMemoryV2.createInMemory()
        assertSame(inMemoryCache1, inMemoryCache2)
    }

    @Test
    fun `createDiskMemory creates and returns disk cache`() {
        val diskCache = fileMemoryV2.createDiskMemory()
        assertNotNull(diskCache)
    }

    @Test
    fun `createDiskMemory returns existing disk cache if already created`() {
        val diskCache1 = fileMemoryV2.createDiskMemory()
        val diskCache2 = fileMemoryV2.createDiskMemory()
        assertSame(diskCache1, diskCache2)
    }

    @Test
    fun `inMemorySize returns the larger of optimistic and minInMemorySizeKB`() {
        assertEquals(1024, fileMemoryV2.inMemorySize())
    }

    @Test
    fun `freeInMemory clears in-memory cache`() {
        // Populate the in-memory cache
        val testKey = "test_key"
        val testData = Pair(byteArrayOf(1, 2, 3), mockk<File>())
        fileMemoryV2.createInMemory().add(testKey, testData)

        // Call freeInMemory
        fileMemoryV2.freeInMemory()

        // Verify that the cache is now empty
        assertNull(fileMemoryV2.createInMemory().get(testKey))
        assertTrue(fileMemoryV2.createInMemory().isEmpty())
    }
}