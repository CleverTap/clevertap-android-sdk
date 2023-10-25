package com.clevertap.android.sdk.utils

import android.util.LruCache
import com.clevertap.android.sdk.Logger

// intended to only hold an gif byte array reference for the life of the parent CTInAppNotification, in order to facilitate parceling
object GifCache {
    private const val MIN_CACHE_SIZE = 1024 * 5 // 5mb minimum (in KB)
    private val maxMemory = Runtime.getRuntime().maxMemory().toInt() / 1024
    private val cacheSize = Math.max(maxMemory / 32, MIN_CACHE_SIZE)
    private var mMemoryCache: LruCache<String, ByteArray>? = null
    @JvmStatic
    fun addByteArray(key: String, byteArray: ByteArray): Boolean {
        if (mMemoryCache == null) {
            return false
        }
        if (getByteArray(key) == null) {
            synchronized(GifCache::class.java) {
                val arraySize = getByteArraySizeInKB(byteArray)
                val available = availableMemory
                Logger.v(
                    "CTInAppNotification.GifCache: gif size: " + arraySize + "KB. Available mem: " + available
                            + "KB."
                )
                if (arraySize > availableMemory) {
                    Logger.v("CTInAppNotification.GifCache: insufficient memory to add gif: $key")
                    return false
                }
                mMemoryCache!!.put(key, byteArray)
                Logger.v("CTInAppNotification.GifCache: added gif for key: $key")
            }
        }
        return true
    }

    @JvmStatic
    fun getByteArray(key: String): ByteArray? {
        synchronized(GifCache::class.java) { return if (mMemoryCache == null) null else mMemoryCache!![key] }
    }

    @JvmStatic
    fun init() {
        synchronized(GifCache::class.java) {
            if (mMemoryCache == null) {
                Logger.v(
                    "CTInAppNotification.GifCache: init with max device memory: $maxMemory KB and allocated cache size: $cacheSize KB"
                )
                try {
                    mMemoryCache = object : LruCache<String, ByteArray>(cacheSize) {
                        override fun sizeOf(key: String, byteArray: ByteArray): Int {
                            // The cache size will be measured in kilobytes rather than
                            // number of items.
                            val size = getByteArraySizeInKB(byteArray)
                            Logger.v("CTInAppNotification.GifCache: have gif of size: " + size + "KB for key: " + key)
                            return size
                        }
                    }
                } catch (t: Throwable) {
                    Logger.v("CTInAppNotification.GifCache: unable to initialize cache: ", t.cause)
                }
            }
        }
    }

    @JvmStatic
    fun removeByteArray(key: String) {
        synchronized(GifCache::class.java) {
            if (mMemoryCache == null) {
                return
            }
            mMemoryCache!!.remove(key)
            Logger.v("CTInAppNotification.GifCache: removed gif for key: $key")
            cleanup()
        }
    }

    private fun cleanup() {
        synchronized(GifCache::class.java) {
            if (isEmpty) {
                Logger.v("CTInAppNotification.GifCache: cache is empty, removing it")
                mMemoryCache = null
            }
        }
    }

    private val availableMemory: Int
        get() {
            synchronized(GifCache::class.java) { return if (mMemoryCache == null) 0 else cacheSize - mMemoryCache!!.size() }
        }

    private fun getByteArraySizeInKB(byteArray: ByteArray): Int {
        return byteArray.size / 1024
    }

    private val isEmpty: Boolean
        get() {
            synchronized(GifCache::class.java) { return mMemoryCache!!.size() <= 0 }
        }
}