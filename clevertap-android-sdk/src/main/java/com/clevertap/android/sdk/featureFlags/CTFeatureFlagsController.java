package com.clevertap.android.sdk.featureFlags;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.FileUtils;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

public class CTFeatureFlagsController {

    private String guid;
    private final CleverTapInstanceConfig config;
    private HashMap<String, Boolean> store;
    private boolean isInitialized = false;
    private final WeakReference<FeatureFlagListener> listenerWeakReference;
    private final Context mContext;

    public CTFeatureFlagsController(Context context, String guid, CleverTapInstanceConfig config, FeatureFlagListener listener) {
        this.guid = guid;
        this.config = config;
        this.store = new HashMap<>();
        listenerWeakReference = new WeakReference<>(listener);
        this.mContext = context.getApplicationContext();
        init();
    }

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    public boolean isInitialized() {
        return isInitialized;
    }

    public Boolean get(String key, boolean defaultValue) {
        if (!isInitialized) {
            getConfigLogger().verbose(getAccountId(), "Feature Flag : Controller not initialized, returning default value - " + defaultValue);
            return defaultValue;
        }
        getConfigLogger().verbose(getAccountId(), "Feature Flag : getting feature flag with key - " + key + " and default value - " + defaultValue);
        Boolean value = store.get(key);
        if (value != null) {
            return store.get(key);
        } else {
            getConfigLogger().verbose(getAccountId(), "Feature Flag : feature flag not found, returning default value - " + defaultValue);
            return defaultValue;
        }
    }

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void setGuidAndInit(String cleverTapID) {
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
            getConfigLogger().verbose(getAccountId(), "Feature Flag : Updating feature flags...");
        } catch (JSONException e) {
            getConfigLogger().verbose(getAccountId(), "Feature Flag : Error parsing Feature Flag array " + e.getLocalizedMessage());
        }
        archiveData(jsonObject);
        notifyFeatureFlagUpdate();
    }

    private void notifyFeatureFlagUpdate() {
        if (listenerWeakReference != null && listenerWeakReference.get() != null) {
            Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listenerWeakReference.get().featureFlagsDidUpdate();
                }
            });
        }
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void fetchFeatureFlags() {
        if (listenerWeakReference != null && listenerWeakReference.get() != null) {
            Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listenerWeakReference.get().fetchFeatureFlags();
                }
            });
        }
    }

    private synchronized void archiveData(JSONObject featureFlagRespObj) {

        if (featureFlagRespObj != null) {
            try {
                FileUtils.writeJsonToFile(mContext, config, getCachedDirName(), getCachedFileName(), featureFlagRespObj);
                getConfigLogger().verbose(getAccountId(), "Feature Flag : feature flags saved into file " + store);
            } catch (Exception e) {
                e.printStackTrace();
                getConfigLogger().verbose(getAccountId(), "Feature Flag : archiveData failed - " + e.getLocalizedMessage());
            }
        }
    }

    private synchronized void init() {
        if (TextUtils.isEmpty(guid))
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void aVoid) {

                try {
                    store.clear();
                    String content = FileUtils.readFromFile(mContext, config, getCachedFullPath());
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
                        getConfigLogger().verbose(getAccountId(), "Feature Flag : feature flags initialized with configs " + store);
                        isInitialized = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getConfigLogger().verbose(getAccountId(), "Feature Flag : unArchiveData failed - " + e.getLocalizedMessage());
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean aBoolean) {

            }
        });

    }

    private Logger getConfigLogger() {
        return config.getLogger();
    }

    private String getAccountId() {
        return config.getAccountId();
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    public void resetWithGuid(String guid) {
        this.guid = guid;
        init();
    }

    private String getCachedFileName() {
        return CTFeatureFlagConstants.CACHED_FILE_NAME;
    }

    private String getCachedDirName() {
        return CTFeatureFlagConstants.DIR_FEATURE_FLAG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getCachedFullPath() {
        return getCachedDirName() + "/" + getCachedFileName();
    }
}
