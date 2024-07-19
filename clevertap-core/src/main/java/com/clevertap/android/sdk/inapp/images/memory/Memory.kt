package com.clevertap.android.sdk.inapp.images.memory

import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
import java.io.File
/**
 * An interface for managing data storage, both in-memory and on disk.
 *
 * Implementations of this interface provide methods for creating and interacting with:
 * - **In-Memory Cache:** A temporary storage using [InMemoryLruCache] for caching key-value pairs,
 *   where the key is of type `T` and the value is a [File].
 * - **Disk-Based Storage:** A persistent storage using [DiskMemory] for storing key-value pairs,
 *   where the key is of type `T` and the value is a [File].
 *
 * @param <T> The type of the key used for storing and retrieving data.
 */
interface Memory<T> {
    /**
     *Creates and returns an in-memory LRU (Least Recently Used) cache.
     *
     * This cache is used for temporary storage of key-value pairs, providing fast access to recently used data.
     *
     * @return An instance of [InMemoryLruCache] representing the in-memory cache.
     */
    fun createInMemory(): InMemoryLruCache<Pair<T, File>>
    /**
     * Creates and returns a disk-based storage mechanism for persisting data.
     *
     * This storage is used for permanent storage of key-value pairs, ensuring data is preserved across app sessions.
     *
     * @return An instance of [DiskMemory] representing the disk-based storage.
     */
    fun createDiskMemory(): DiskMemory
    /**
     * Returns the size of the in-memory cache.
     *
     * The meaning of "size" may vary depending on the specific implementation of [InMemoryLruCache].
     * It could represent the number of entries or the total memory consumed.
     *
     * @return The size of the in-memory cache.
     */
    fun inMemorySize(): Int
    /**
     * Frees or clears the in-memory cache, releasing the resources it holds.
     *
     * This is typically used to reclaim memory when it is no longer needed.
     */
    fun freeInMemory()
}
