package com.clevertap.android.sdk.task;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to create & cache Executors{@link CTExecutors}
 * Every account has it's dedicated Executor
 */
public class CTExecutorFactory {

    private static final Map<String, CTExecutors> executorMap = Collections
            .synchronizedMap(new HashMap<String, CTExecutors>());

    public static CTExecutors executors(CleverTapInstanceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Can't create task for null config");
        }
        CTExecutors executorForAccount = executorMap.get(config.getAccountId());
        if (executorForAccount == null) {
            synchronized (CTExecutorFactory.class) {
                executorForAccount = executorMap.get(config.getAccountId());
                if (executorForAccount == null) {
                    executorForAccount = new CTExecutors(config);
                    executorMap.put(config.getAccountId(), executorForAccount);
                }
            }
        }
        return executorForAccount;
    }
}