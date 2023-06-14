package com.clevertap.android.sdk;

import android.content.Context;

import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CleverTapManager {
    static CleverTapInstanceConfig defaultConfig;
    private static String sdkVersion;  // For Google Play Store/Android Studio analytics
    private static Map<String, CleverTapAPI> instances;

    public static CleverTapAPI instanceWithConfig(Context context, CleverTapInstanceConfig config, String cleverTapID) {
        if (config == null) {
            Logger.v("CleverTapInstanceConfig cannot be null");
            return null;
        }

        if (instances == null) {
            instances = new ConcurrentHashMap<>();
        }

        CleverTapAPI instance = instances.get(config.getAccountId());
        if (instance == null) {
            instance = createInstance(context, config, cleverTapID);
            instances.put(config.getAccountId(), instance);
            final CleverTapAPI finalInstance = instance;
            Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
            task.execute("recordDeviceIDErrors", new Callable<Void>() {
                @Override
                public Void call() {
                    if (finalInstance.getCleverTapID() != null) {
                        finalInstance.coreState.getLoginController().recordDeviceIDErrors();
                    }
                    return null;
                }
            });
        } else if (instance.isErrorDeviceId() && instance.getConfig().getEnableCustomCleverTapId()
                && Utils.validateCTID(cleverTapID)) {
            instance.coreState.getLoginController().asyncProfileSwitchUser(null, null, cleverTapID);
        }
        Logger.v(config.getAccountId() + ": async_deviceID", "CleverTapAPI instance = " + instance);
        return instance;
    }

    private static CleverTapAPI createInstance(Context context, CleverTapInstanceConfig config, String cleverTapID) {
        return new CleverTapAPI(context, config, cleverTapID);
    }

    public static CleverTapAPI createInstanceIfAvailable(Context context, String _accountId, String cleverTapID) {
        try {
            if (_accountId == null) {
                try {
                    return getDefaultInstance(context, cleverTapID);
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
            String configJson = StorageHelper.getString(context, "instance:" + _accountId, "");
            if (!configJson.isEmpty()) {
                CleverTapInstanceConfig config = CleverTapInstanceConfig.createInstance(configJson);
                Logger.v("Inflated Instance Config: " + configJson);
                return config != null ? instanceWithConfig(context, config, cleverTapID) : null;
            } else {
                try {
                    CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
                    return (instance != null && instance.coreState.getConfig().getAccountId().equals(_accountId))
                            ? instance : null;
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static CleverTapAPI fromAccountId(Context context, String _accountId) {
        if (instances == null) {
            return createInstanceIfAvailable(context, _accountId);
        }

        for (String accountId : instances.keySet()) {
            CleverTapAPI instance = instances.get(accountId);
            boolean shouldProcess = false;
            if (instance != null) {
                shouldProcess = (_accountId == null && instance.coreState.getConfig().isDefaultInstance())
                        || instance
                        .getAccountId()
                        .equals(_accountId);
            }
            if (shouldProcess) {
                return instance;
            }
        }

        return null;// failed to get instance
    }

    private static CleverTapAPI createInstanceIfAvailable(Context context, String _accountId) {
        return createInstanceIfAvailable(context, _accountId, null);
    }

    public static CleverTapAPI getDefaultInstance(Context context, String cleverTapID) {
        // For Google Play Store/Android Studio tracking
        sdkVersion = BuildConfig.SDK_VERSION_STRING;

        if (defaultConfig != null) {
            return instanceWithConfig(context, defaultConfig, cleverTapID);
        } else {
            defaultConfig = getDefaultConfig(context);
            if (defaultConfig != null) {
                return instanceWithConfig(context, defaultConfig, cleverTapID);
            }
        }
        return null;
    }

    private static CleverTapInstanceConfig getDefaultConfig(Context context) {
        ManifestInfo manifest = ManifestInfo.getInstance(context);
        String accountId = manifest.getAccountId();
        String accountToken = manifest.getAcountToken();
        String accountRegion = manifest.getAccountRegion();
        if (accountId == null || accountToken == null) {
            Logger.i(
                    "Account ID or Account token is missing from AndroidManifest.xml, unable to create default instance");
            return null;
        }
        if (accountRegion == null) {
            Logger.i("Account Region not specified in the AndroidManifest - using default region");
        }

        return CleverTapInstanceConfig.createDefaultInstance(context, accountId, accountToken, accountRegion);

    }

}

