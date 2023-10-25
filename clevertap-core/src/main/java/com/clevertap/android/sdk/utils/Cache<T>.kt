package com.clevertap.android.sdk.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.util.lruCache

class Cache<T : Any>(
    private val maxSize: Int
) {

    companion object {
        const val TYPE_LRU = "TYPE_LRU"

    }

    private val memoryCache: LruCache<String, T> = lruCache(
        maxSize = maxSize,
        sizeOf = { _, v ->
            return@lruCache v.sizeInKb()
        }
    )

    fun add(key: String, value: T) : Boolean {
        if (value.sizeInKb() > maxSize) {
            return false
        }
        memoryCache.put(key, value)
        return true
    }

    fun get(key: String): T? {
        return memoryCache.get(key)
    }

    fun remove(key: String): T? {
        return memoryCache.remove(key)
    }

    fun empty() {
        memoryCache.evictAll()
    }
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