package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.TestLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * We do not validate file read/write in unit tests, we will have to make instrumentation tests for
 * the same
 *
 * https://stackoverflow.com/questions/43430148/how-to-test-file-io-in-android-unittest-androidtest
 */
class FileCacheTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cacheDirectory: File
    private lateinit var fileCache: FileCache
    private val mockLogger = TestLogger()
    private val maxFileSizeKb = 1024 // 1MB

    @Before
    fun setup() {
        cacheDirectory = tempFolder.newFolder("test_cache")
        fileCache = FileCache(cacheDirectory, maxFileSizeKb, mockLogger)
    }

    @Test
    fun `add successfully caches file`() {
        val key = "test_key"
        val data = "Test data".toByteArray()

        val result = fileCache.add(key, data)

        assertTrue(result)
        val cachedFile = File(cacheDirectory, "CT_FILE_${fileCache.hashFunction(key)}")
        assertTrue(cachedFile.exists())
        assertArrayEquals(data, cachedFile.readBytes())
    }

    @Test
    fun `add fails when file size exceeds limit`() {
        val key = "test_key"
        val data = ByteArray(maxFileSizeKb * 1024 + 1024) // Exceeds limit by 1 kb

        val result = fileCache.add(key, data)

        assertFalse(result)
        val cachedFile = File(cacheDirectory, "CT_FILE_${fileCache.hashFunction(key)}")
        assertFalse(cachedFile.exists()) // File should not be cached
    }

    @Test
    fun `addAndReturnFileInstance returns correct File instance`() {
        val key = "test_key"
        val data = "Test data".toByteArray()

        val cachedFile = fileCache.addAndReturnFileInstance(key, data)

        assertNotNull(cachedFile)
        assertTrue(cachedFile.exists())
        assertEquals(File(cacheDirectory, "CT_FILE_${fileCache.hashFunction(key)}"), cachedFile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addAndReturnFileInstance throws exception when file size exceeds limit`() {
        val key = "test_key"
        val data = ByteArray(maxFileSizeKb * 1024 + 1024) // Exceeds limit by 1 kb

        fileCache.addAndReturnFileInstance(key, data) // Should throw exception
    }

    @Test
    fun `get returns cached file`() {
        val key = "test_key"
        val data = "Test data".toByteArray()
        fileCache.add(key, data)

        val cachedFile = fileCache.get(key)

        assertNotNull(cachedFile)
        assertTrue(cachedFile!!.exists())
        assertArrayEquals(data, cachedFile.readBytes())
    }

    @Test
    fun `get returns null for non-existent key`() {
        val key = "non_existent_key"

        val cachedFile = fileCache.get(key)

        assertNull(cachedFile)
    }

    @Test
    fun `remove deletes cached file`() {
        val key = "test_key"
        val data = "Test data".toByteArray()
        fileCache.add(key, data)

        val result = fileCache.remove(key)

        assertTrue(result)
        val cachedFile = File(cacheDirectory, "CT_FILE_${fileCache.hashFunction(key)}")
        assertFalse(cachedFile.exists())
    }

    @Test
    fun `remove returns false for non-existent key`() {
        val key = "non_existent_key"

        val result = fileCache.remove(key)

        assertFalse(result)
    }

    @Test
    fun `empty clears the cache directory`() {
        val key1 = "test_key1"
        val key2 = "test_key2"
        fileCache.add(key1, "Test data 1".toByteArray())
        fileCache.add(key2, "Test data 2".toByteArray())

        val result = fileCache.empty()

        assertTrue(result)
        assertTrue(cacheDirectory.listFiles().isNullOrEmpty()) // Directory should be empty
    }

}