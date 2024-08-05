package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.util.lruCache
/**
 * An in-memory LRU (Least Recently Used) cache implementation.
 *
 * This class provides a simple way to cache objects in memory, using a configurable maximum size.
 * It utilizes the [LruCache] class from the Android framework to manage the eviction of least recently used items.
 *
 * @param <T> The type of objects to be cached.
 * @param maxSize The maximum size of the cache in kilobytes.
 * @param memoryCache An optional implementation of [CacheMethods] for customizing the caching behavior.
 */
class InMemoryLruCache<T : Any>(
    private val maxSize: Int,
    private val memoryCache: CacheMethods<T> = object : CacheMethods<T> {

        val lru = LruCacheProvider.provide<T>(maxSize)

        override fun add(key: String, value: T): Boolean {
            lru.put(key, value) // we assume there is no failure in addition
            return true
        }

        override fun get(key: String): T? = lru.get(key)

        override fun remove(key: String): T? = lru.remove(key)

        override fun empty() = lru.evictAll()

        override fun isEmpty(): Boolean = lru.size() == 0
    }
) {

    companion object {
        const val TYPE_LRU = "TYPE_LRU"
    }

    /**
     * Adds an object to the cache.
     *
     * If the size of the object (in kilobytes) exceeds the maximum cache size, it will not be added,
     * and any existing entry with the same key will be removed.
     *
     * @param key The unique key associated with the object.
     * @param value The object to be cached.
     * @return `true` if the object was successfully added, `false` otherwise.
     */
    fun add(key: String, value: T) : Boolean {
        if (value.sizeInKb() > maxSize) {
            remove(key = key)
            return false
        }
        memoryCache.add(key, value)
        return true
    }

    /**
     * Retrieves an object from the cache.
     *
     * @param key The unique key associated with the object.
     * @return The cached object if found, `null` otherwise.
     */
    fun get(key: String): T? {
        return memoryCache.get(key)
    }

    /**
     * Removes an object from the cache.
     *
     * @param key The unique key associated with the object.
     * @return The removed object if found, `null` otherwise.
     */
    fun remove(key: String): T? {
        return memoryCache.remove(key)
    }

    /**
     * Clears the entire cache, removing all cached objects.
     */
    fun empty() {
        memoryCache.empty()
    }

    /**
     * Checks if the cache is empty.
     *
     * @return `true` if the cache is empty, `false` otherwise.
     */
    fun isEmpty() = memoryCache.isEmpty()
}

/**
 * Extension function to calculate the size of an object in kilobytes.
 *
 * Supports [Bitmap] and [ByteArray] types. For other types, returns a default size of 1 kilobyte.
 *
 * @return The size of the object in kilobytes.
 */
fun Any?.sizeInKb() : Int = when (this) {
    is Bitmap -> {
        byteCount / 1024
    }
    is ByteArray -> {
        size / 1024
    }
    else -> {
        1
    }
}

/**
 * Object providing a utility function to create an [LruCache] instance.
 */
object LruCacheProvider {
    /**
     * Creates and returns an [LruCache] instance with the specified maximum size.
     *
     * The [LruCache.sizeOf] function is used to calculate the size of each cached object in kilobytes.
     *
     * @param maxSize The maximum size of the cache in kilobytes.
     * @return An instance of [LruCache].
     */
    fun <T: Any> provide(maxSize: Int) : LruCache<String, T> {
        return lruCache(
           maxSize = maxSize,
           sizeOf = { _, v ->
               return@lruCache v.sizeInKb()
           }
        )
    }
}

/**
 * Interface defining methods for interacting with a cache.
 *
 * @param <T> The type of objects stored in the cache.
 */
interface CacheMethods<T> {
    /**
     * Adds an object to the cache.
     *
     * @param key The unique key associated with the object.
     * @param value The object to be cached.
     * @return `true` if the object was successfully added, `false` otherwise.
     */
    fun add(key: String, value: T) : Boolean
    /**
     * Retrieves an object from the cache.
     *
     * @param key The unique key associated with the object.
     * @return The cached object if found, `null` otherwise.
     */
    fun get(key: String): T?
    /**
     * Removes an object from the cache.
     *
     * @param key The unique key associated with the object.
     * @return The removed object if found, `null` otherwise.
     */
    fun remove(key: String): T?
    /**
     * Clears the entire cache, removing all cached objects.
     */
    fun empty()
    /**
     * Checks if the cache is empty.
     *
     * @return `true` if the cache is empty, `false` otherwise.
     */
    fun isEmpty(): Boolean
}