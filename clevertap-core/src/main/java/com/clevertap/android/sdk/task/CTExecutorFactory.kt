package com.clevertap.android.sdk.task

import android.os.Build
import com.clevertap.android.sdk.CleverTapInstanceConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory class to create & cache Executors [CTExecutors]
 * Every account has its dedicated Executor
 * Uses API-level optimized approaches with lifecycle management
 */
object CTExecutorFactory {

    private const val TAG_RESOURCE_DOWNLOADER = "Resource Downloader"

    // Use ConcurrentHashMap instead of synchronized HashMap
    private val executorMap = ConcurrentHashMap<String, CTExecutors>()

    @JvmStatic
    fun executors(config: CleverTapInstanceConfig?): CTExecutors {
        requireNotNull(config) { "Can't create task for null config" }

        val accountId = config.accountId

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
            // Use computeIfAbsent for optimal performance on API 24+
            executorMap.computeIfAbsent(accountId) { CTExecutors(config) }
        } else {
            // Use putIfAbsent approach for API 21-23
            getOrCreateExecutorApi21(accountId) { CTExecutors(config) }
        }
    }

    @JvmStatic
    fun executorResourceDownloader(): CTExecutors = executorResourceDownloader(8)

    @JvmStatic
    fun executorResourceDownloader(ioPoolSize: Int): CTExecutors {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
            // Use computeIfAbsent for optimal performance on API 24+
            executorMap.computeIfAbsent(TAG_RESOURCE_DOWNLOADER) { CTExecutors(ioPoolSize) }
        } else {
            // Use putIfAbsent approach for API 21-23
            getOrCreateExecutorApi21(TAG_RESOURCE_DOWNLOADER) { CTExecutors(ioPoolSize) }
        }
    }

    /**
     * API 21-23 compatible method using putIfAbsent
     * Handles the case where computeIfAbsent is not available
     */
    private fun getOrCreateExecutorApi21(key: String, supplier: () -> CTExecutors): CTExecutors {
        // Check if executor already exists (lock-free read)
        var existingExecutor = executorMap[key]
        if (existingExecutor == null) {
            // Create new executor and try to put it atomically
            val newExecutor = supplier()
            existingExecutor = executorMap.putIfAbsent(key, newExecutor)
            if (existingExecutor == null) {
                // Our executor was successfully added
                existingExecutor = newExecutor
            }
            // If another thread won the race, the unused executor will be garbage collected
        }
        return existingExecutor
    }

    /**
     * Remove executor for specific account (for cleanup)
     */
    @JvmStatic
    fun removeExecutor(accountId: String): Boolean {
        val removed = executorMap.remove(accountId)
        return removed != null
    }
}