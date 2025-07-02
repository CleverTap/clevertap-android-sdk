package com.clevertap.android.sdk.task;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class to create & cache Executors{@link CTExecutors}
 * Every account has it's dedicated Executor
 */
public class CTExecutorFactory {

    private static final String TAG_RESOURCE_DOWNLOADER = "Resource Downloader";

    private static final ConcurrentHashMap<String, CTExecutors> executorMap = new ConcurrentHashMap<>();

    public static CTExecutors executors(CleverTapInstanceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Can't create task for null config");
        }

        String accountId = config.getAccountId();

        // lock-free read
        CTExecutors executorForAccount = executorMap.get(accountId);
        if (executorForAccount == null) {
            CTExecutors newExecutor = new CTExecutors(config);
            executorForAccount = executorMap.putIfAbsent(accountId, newExecutor);
            if (executorForAccount == null) {
                executorForAccount = newExecutor;
            }
        }
        return executorForAccount;
    }

    public static CTExecutors executorResourceDownloader() {
        return executorResourceDownloader(8);
    }

    public static CTExecutors executorResourceDownloader(int ioPoolSize) {
        // lock-free read
        CTExecutors executorForAccount = executorMap.get(TAG_RESOURCE_DOWNLOADER);
        if (executorForAccount == null) {
            CTExecutors newExecutor = new CTExecutors(ioPoolSize);
            executorForAccount = executorMap.putIfAbsent(TAG_RESOURCE_DOWNLOADER, newExecutor);
            if (executorForAccount == null) {
                executorForAccount = newExecutor;
            }
        }
        return executorForAccount;
    }
}