package com.clevertap.android.sdk.task;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import java.util.HashMap;

public class CTExecutorFactory {

    private static volatile HashMap<String, CTExecutors> executorMap = new HashMap<>();

    public static CTExecutors getInstance(CleverTapInstanceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Can't create task for null config");
        }
        CTExecutors executorForAccount = executorMap.get(config.getAccountId());
        if (executorForAccount == null) {
            synchronized (CTExecutorFactory.class) {
                executorForAccount = executorMap.get(config.getAccountId());
                if (executorForAccount == null) {
                    executorForAccount = new CTExecutors();
                    executorMap.put(config.getAccountId(), executorForAccount);
                }
            }
        }
        return executorForAccount;
    }

}