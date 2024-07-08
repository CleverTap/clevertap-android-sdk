package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import java.io.File

interface Memory<T> {

    /**
     * Creates and returns an in-memory LRU cache.
     * The cache stores pairs of type (T, File).
     */
    fun createInMemory(): LruCache<Pair<T, File>>

    /**
     * Creates and returns a disk-based file cache.
     */
    fun createDiskMemory(): FileCache// TODO: rename FileCache to DiskMemory

    /**
     * Returns the size of the in-memory cache.
     */
    fun inMemorySize(): Int

    /**
     * Frees or clears the in-memory cache.
     */
    fun freeInMemory()
}
