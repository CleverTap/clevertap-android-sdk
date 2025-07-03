package com.clevertap.android.sdk.task;

import android.os.Build;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class to create & cache Executors{@link CTExecutors}
 * Every account has its dedicated Executor
 * Uses API-level optimized approaches
 */
public class CTExecutorFactory {

    private static final String TAG_RESOURCE_DOWNLOADER = "Resource Downloader";

    private static final ConcurrentHashMap<String, CTExecutors> executorMap = new ConcurrentHashMap<>();

    public static CTExecutors executors(CleverTapInstanceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Can't create task for null config");
        }

        String accountId = config.getAccountId();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
            // Use computeIfAbsent for optimal performance on API 24+
            return executorMap.computeIfAbsent(accountId, key -> new CTExecutors(config));
        } else {
            // Use putIfAbsent approach for API 21-23
            return getOrCreateExecutorApi21(accountId, () -> new CTExecutors(config));
        }
    }

    public static CTExecutors executorResourceDownloader() {
        return executorResourceDownloader(8);
    }

    public static CTExecutors executorResourceDownloader(int ioPoolSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
            // Use computeIfAbsent for optimal performance on API 24+
            return executorMap.computeIfAbsent(TAG_RESOURCE_DOWNLOADER, key -> new CTExecutors(ioPoolSize));
        } else {
            // Use putIfAbsent approach for API 21-23
            return getOrCreateExecutorApi21(TAG_RESOURCE_DOWNLOADER, () -> new CTExecutors(ioPoolSize));
        }
    }

    /**
     * API 21-23 compatible method using putIfAbsent
     * Handles the case where computeIfAbsent is not available
     */
    private static CTExecutors getOrCreateExecutorApi21(String key, ExecutorSupplier supplier) {
        // Check if executor already exists (lock-free read)
        CTExecutors existingExecutor = executorMap.get(key);
        if (existingExecutor == null) {
            // Create new executor and try to put it atomically
            CTExecutors newExecutor = supplier.create();
            existingExecutor = executorMap.putIfAbsent(key, newExecutor);
            if (existingExecutor == null) {
                // Our executor was successfully added
                existingExecutor = newExecutor;
            }
            // If another thread won the race, the unused executor will be garbage collected
        }
        return existingExecutor;
    }

    /**
     * Remove executor for specific account (for cleanup)
     */
    public static boolean removeExecutor(String accountId) {
        CTExecutors removed = executorMap.remove(accountId);
        return removed != null;
    }

    /**
     * Functional interface for executor creation
     * Compatible with API 21+ (avoiding Java 8 Function interface)
     */
    private interface ExecutorSupplier {
        CTExecutors create();
    }
}