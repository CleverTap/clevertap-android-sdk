package com.clevertap.android.sdk.featureFlags;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

import android.text.TextUtils;
import com.clevertap.android.sdk.BaseAnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.OnSuccessListener;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.FileUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CTFeatureFlagsController {

    final CleverTapInstanceConfig config;

    public String getGuid() {
        return guid;
    }

    String guid;

    boolean isInitialized = false;

    final BaseAnalyticsManager mAnalyticsManager;

    final BaseCallbackManager mCallbackManager;

    FileUtils mFileUtils;

    private final Map<String, Boolean> store = Collections.synchronizedMap(new HashMap<String, Boolean>());

    CTFeatureFlagsController(String guid, CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager, BaseAnalyticsManager analyticsManager, FileUtils fileUtils) {
        this.guid = guid;
        this.config = config;
        mCallbackManager = callbackManager;
        mAnalyticsManager = analyticsManager;
        mFileUtils = fileUtils;
        init();
    }

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void fetchFeatureFlags() {
        Task<Void> task = CTExecutorFactory.executors(config).mainTask();
        task.execute("fetchFeatureFlags", new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    mAnalyticsManager.fetchFeatureFlags();
                } catch (Exception e) {
                    getConfigLogger().verbose(getLogTag(), e.getLocalizedMessage());
                }
                return null;
            }
        });
    }

    /**
     * Getter to return the feature flag configured at the dashboard
     *
     * @param key          - Key of the Feature flag
     * @param defaultValue - default value of the Key, in case we don't find any Feature Flag with the Key.
     * @return boolean- Value of the Feature flag.
     */
    public Boolean get(String key, boolean defaultValue) {
        if (!isInitialized) {
            getConfigLogger()
                    .verbose(getLogTag(), "Controller not initialized, returning default value - " + defaultValue);
            return defaultValue;
        }
        getConfigLogger().verbose(getLogTag(),
                "Getting feature flag with key - " + key + " and default value - " + defaultValue);
        Boolean value = store.get(key);
        if (value != null) {
            return value;
        } else {
            getConfigLogger()
                    .verbose(getLogTag(), "Feature flag not found, returning default value - " + defaultValue);
            return defaultValue;
        }
    }

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    /**
     * Method to check Feature Flag has been initialized
     *
     * @return boolean- true if initialized, else false.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void resetWithGuid(String guid) {
        this.guid = guid;
        init();
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void setGuidAndInit(String cleverTapID) {
        if (isInitialized) {
            return;
        }
        this.guid = cleverTapID;
        init();
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public synchronized void updateFeatureFlags(JSONObject jsonObject) throws JSONException {

        JSONArray featureFlagList = jsonObject.getJSONArray(Constants.KEY_KV);
        try {
            for (int i = 0; i < featureFlagList.length(); i++) {
                JSONObject ff = featureFlagList.getJSONObject(i);
                store.put(ff.getString("n"), ff.getBoolean("v"));
            }
            getConfigLogger().verbose(getLogTag(), "Updating feature flags..." + store);
        } catch (JSONException e) {
            getConfigLogger().verbose(getLogTag(), "Error parsing Feature Flag array " + e.getLocalizedMessage());
        }
        archiveData(jsonObject);
        notifyFeatureFlagUpdate();
    }

    String getCachedDirName() {
        return CTFeatureFlagConstants.DIR_FEATURE_FLAG + "_" + config.getAccountId() + "_" + guid;
    }

    String getCachedFileName() {
        return CTFeatureFlagConstants.CACHED_FILE_NAME;
    }

    String getCachedFullPath() {
        return getCachedDirName() + "/" + getCachedFileName();
    }

    void init() {
        if (TextUtils.isEmpty(guid)) {
            return;
        }
        Task<Boolean> task = CTExecutorFactory.executors(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(final Boolean init) {
                isInitialized = init;
            }
        });
        task.execute("initFeatureFlags", new Callable<Boolean>() {
            @Override
            public Boolean call() {
                synchronized (this) {
                    getConfigLogger().verbose(getLogTag(), "Feature flags init is called");
                    String fileName = getCachedFullPath();
                    try {
                        store.clear();
                        String content = mFileUtils.readFromFile(fileName);
                        if (!TextUtils.isEmpty(content)) {

                            JSONObject jsonObject = new JSONObject(content);
                            JSONArray kvArray = jsonObject.getJSONArray(Constants.KEY_KV);

                            if (kvArray != null && kvArray.length() > 0) {
                                for (int i = 0; i < kvArray.length(); i++) {
                                    JSONObject object = (JSONObject) kvArray.get(i);
                                    if (object != null) {

                                        String Key = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY);
                                        String Value = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE);
                                        if (!TextUtils.isEmpty(Key)) {
                                            store.put(Key, Boolean.parseBoolean(Value));
                                        }
                                    }
                                }
                            }
                            getConfigLogger().verbose(getLogTag(), "Feature flags initialized from file " + fileName +
                                    " with configs  " + store);
                        } else {
                            getConfigLogger().verbose(getLogTag(), "Feature flags file is empty-" + fileName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        getConfigLogger().verbose(getLogTag(),
                                "UnArchiveData failed file- " + fileName + " " + e.getLocalizedMessage());
                        return false;
                    }
                    return true;
                }
            }
        });
    }

    private synchronized void archiveData(JSONObject featureFlagRespObj) {

        if (featureFlagRespObj != null) {
            try {
                mFileUtils.writeJsonToFile(getCachedDirName(), getCachedFileName(),
                        featureFlagRespObj);
                getConfigLogger()
                        .verbose(getLogTag(), "Feature flags saved into file-[" + getCachedFullPath() + "]" + store);
            } catch (Exception e) {
                e.printStackTrace();
                getConfigLogger().verbose(getLogTag(), "ArchiveData failed - " + e.getLocalizedMessage());
            }
        }
    }

    private Logger getConfigLogger() {
        return config.getLogger();
    }

    private String getLogTag() {
        return config.getAccountId() + "[Feature Flag]";
    }

    private void notifyFeatureFlagUpdate() {
        if (mCallbackManager.getFeatureFlagListener() != null) {
            Task<Void> task = CTExecutorFactory.executors(config).mainTask();
            task.execute("notifyFeatureFlagUpdate", new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        if (mCallbackManager.getFeatureFlagListener() != null) {
                            mCallbackManager.getFeatureFlagListener().featureFlagsUpdated();
                        }
                    } catch (Exception e) {
                        getConfigLogger().verbose(getLogTag(), e.getLocalizedMessage());
                    }
                    return null;
                }
            });
        }
    }
}