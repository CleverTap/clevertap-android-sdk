package com.clevertap.android.sdk.utils

class TestCacheProvider<T> {

    val cache = hashMapOf<String, T>()
    fun provide() = object : CacheMethods<T> {
        override fun add(key: String, value: T): Boolean {
            cache[key] = value
            return true
        }

        override fun get(key: String): T? = cache[key]

        override fun remove(key: String): T? = cache.remove(key)

        override fun empty() {
            cache.clear()
        }

        override fun isEmpty(): Boolean = cache.size == 0
    }
}