package com.clevertap.android.sdk.featureFlags;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

import android.content.Context;
import android.text.TextUtils;
import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.FileUtils;
import com.clevertap.android.sdk.utils.Utils;
import java.util.HashMap;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CTFeatureFlagsController {

    private final CleverTapInstanceConfig config;

    private String guid;

    private boolean isInitialized = false;

    private final AnalyticsManager mAnalyticsManager;

    private final BaseCallbackManager mCallbackManager;

    private final Context mContext;

    private final HashMap<String, Boolean> store;

    public CTFeatureFlagsController(Context context, String guid, CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager, AnalyticsManager analyticsManager) {
        this.guid = guid;
        this.config = config;
        this.store = new HashMap<>();
        this.mContext = context.getApplicationContext();
        mCallbackManager = callbackManager;
        mAnalyticsManager = analyticsManager;
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
        Utils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAnalyticsManager.fetchFeatureFlags();
                } catch (Exception e) {
                    getConfigLogger().verbose(getLogTag(), e.getLocalizedMessage());
                }
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
            return store.get(key);
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
    public void updateFeatureFlags(JSONObject jsonObject) throws JSONException {

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

    private synchronized void archiveData(JSONObject featureFlagRespObj) {

        if (featureFlagRespObj != null) {
            try {
                new FileUtils(mContext, config).writeJsonToFile(getCachedDirName(), getCachedFileName(),
                        featureFlagRespObj);
                getConfigLogger()
                        .verbose(getLogTag(), "Feature flags saved into file-[" + getCachedFullPath() + "]" + store);
            } catch (Exception e) {
                e.printStackTrace();
                getConfigLogger().verbose(getLogTag(), "ArchiveData failed - " + e.getLocalizedMessage());
            }
        }
    }

    private String getCachedDirName() {
        return CTFeatureFlagConstants.DIR_FEATURE_FLAG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getCachedFileName() {
        return CTFeatureFlagConstants.CACHED_FILE_NAME;
    }

    private String getCachedFullPath() {
        return getCachedDirName() + "/" + getCachedFileName();
    }

    private Logger getConfigLogger() {
        return config.getLogger();
    }

    private String getLogTag() {
        return config.getAccountId() + "[Feature Flag]";
    }

    private synchronized void init() {
        if (TextUtils.isEmpty(guid)) {
            return;
        }
        Task<Boolean> task = CTExecutorFactory.getInstance(config).ioTask();
        task.call(new Callable<Boolean>() {
            @Override
            public Boolean call(){
                getConfigLogger().verbose(getLogTag(), "Feature flags init is called");
                String fileName = getCachedFullPath();
                try {
                    store.clear();
                    String content = new FileUtils(mContext, config).readFromFile(fileName);
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
                        isInitialized = true;
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
        });
    }

    private void notifyFeatureFlagUpdate() {
        if (mCallbackManager.getFeatureFlagListener() != null) {
            Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mCallbackManager.getFeatureFlagListener() != null) {
                            mCallbackManager.getFeatureFlagListener().featureFlagsUpdated();
                        }
                    } catch (Exception e) {
                        getConfigLogger().verbose(getLogTag(), e.getLocalizedMessage());
                    }
                }
            });
        }
    }
}