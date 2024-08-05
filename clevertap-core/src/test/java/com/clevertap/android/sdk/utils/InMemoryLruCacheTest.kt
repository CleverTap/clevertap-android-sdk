package com.clevertap.android.sdk.utils

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InMemoryLruCacheTest {

    private val inMemoryLruCache = InMemoryLruCache<ByteArray>(
        maxSize = 100,
        memoryCache = TestCacheProvider<ByteArray>().provide()
    )

    private val validData = byteArrayOf(0)
    private val sizeExceedingData = ByteArray(101 * 1024) { index ->
        index.toByte()
    }

    @Before
    fun before() {
        inMemoryLruCache.empty()
    }

    private val cacheKey = "key"

    @Test
    fun `test add works correctly`() {
        val result = inMemoryLruCache.add(cacheKey, validData)
        assertEquals(true, result)
    }

    @Test
    fun `test add does not allow greater than allowed size`() {
        val result = inMemoryLruCache.add(cacheKey, sizeExceedingData)
        assertEquals(false, result)
    }

    @Test
    fun `getter returns null if there is no data`() {
        val result = inMemoryLruCache.get(cacheKey)
        assertNull(result)
    }

    @Test
    fun `getter returns valid data if it exists`() {

        // warm up cache
        inMemoryLruCache.add(cacheKey, validData)

        val result = inMemoryLruCache.get(cacheKey)
        assertNotNull(result)
        assertEquals(result, validData)
    }

    @Test
    fun `remove method evicts data`() {

        // warm cache
        inMemoryLruCache.add(cacheKey, validData)

        val op = inMemoryLruCache.remove(cacheKey)
        assertEquals(validData, op)
    }

    @Test
    fun `remove method returns null if there is no data`() {

        val op = inMemoryLruCache.remove(cacheKey)
        assertNull(op)
    }

    @Test
    fun `empty method clears cache`() {
        inMemoryLruCache.add("key1", validData)

        inMemoryLruCache.empty()
        assertEquals(true, inMemoryLruCache.isEmpty())
    }

}