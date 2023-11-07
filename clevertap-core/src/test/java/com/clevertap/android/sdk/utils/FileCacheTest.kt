package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FileCacheTest {

    private val fileCache: FileCache = FileCache(
        directory = File("/"),
        maxFileSizeKb = 500,
        logger = TestLogger()
    )

    @Test
    @Throws(Exception::class)
    fun testAdd() {
        val result = fileCache.add("key", byteArrayOf(0.toByte()))
        assertEquals(true, result)
    }

    @Test
    @Throws(Exception::class)
    fun `add method fails if size is greater than allowed size`() {
        val largeBytes = ByteArray(1000 * 1024) { index ->
            index.toByte()
        }
        val result = fileCache.add("key", largeBytes)
        assertEquals(false, result)
    }

    @Test
    @Throws(Exception::class)
    fun testGet() {
        fileCache.add("key", byteArrayOf(0.toByte()))
        val result = fileCache.get("key")
        assertNotNull(result)
        assertEquals(result.exists(), true)
    }

    @Test
    @Throws(Exception::class)
    fun `file cache returns null when there is no file for url`() {
        val result = fileCache.get("non-cached-key-res")
        assertNull(result)
    }

    @Test
    @Throws(Exception::class)
    fun testRemove() {
        fileCache.add("key", byteArrayOf(0.toByte()))
        val result = fileCache.remove("key")
        assertEquals(true, result)
    }

    @Test
    @Throws(Exception::class)
    fun `remove returns false when we try to remove file which is not there in cache`() {
        val result = fileCache.remove("non-cached-key-res")
        assertEquals(true, result)
    }

    @Test
    @Throws(Exception::class)
    fun `empty deletes all files`() {

        // warm up cache
        fileCache.add("key1", byteArrayOf(0.toByte()))
        fileCache.add("key2", byteArrayOf(0.toByte()))

        // check it is warmed up
        val get = fileCache.get("key1")
        assertNotNull(get)

        // clear cache
        val result = fileCache.empty()
        assertEquals(true, result)
    }
}