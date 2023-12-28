package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.util.lruCache

class LruCache<T : Any>(
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

    fun add(key: String, value: T) : Boolean {
        if (value.sizeInKb() > maxSize) {
            return false
        }
        memoryCache.add(key, value)
        return true
    }

    fun get(key: String): T? {
        return memoryCache.get(key)
    }

    fun remove(key: String): T? {
        return memoryCache.remove(key)
    }

    fun empty() {
        memoryCache.empty()
    }

    fun isEmpty() = memoryCache.isEmpty()
}

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

object LruCacheProvider {
    fun <T: Any> provide(maxSize: Int) : LruCache<String, T> {
        return lruCache(
           maxSize = maxSize,
           sizeOf = { _, v ->
               return@lruCache v.sizeInKb()
           }
        )
    }
}

interface CacheMethods<T> {
    fun add(key: String, value: T) : Boolean
    fun get(key: String): T?
    fun remove(key: String): T?
    fun empty()
    fun isEmpty(): Boolean
}