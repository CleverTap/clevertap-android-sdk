package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

public final class CTProductConfigController {

    private String guid;

    public boolean isInitialized() {
        return isInitialized;
    }

    private boolean isInitialized = false;
    private final CleverTapInstanceConfig config;
    private final Context context;
    private HashMap<String, String> defaultConfig;
    private final HashMap<String, String> activatedConfig = new HashMap<>();
    private final Listener listener;
    private long minFetchIntervalInSeconds;
    private long lastFetchTimeStampInMillis;
    private boolean isFetching = false;
    private boolean isActivating = false;

    private int[] arpValues = new int[]{5, 60};//0 is for rc_n, 1 is for rc_w

    public CTProductConfigController(Context context, String guid, CleverTapInstanceConfig config, Listener listener) {
        this.context = context;
        this.guid = guid;
        this.config = config;
        this.listener = listener;
        initAsync();
    }

    private synchronized void initAsync() {
        if (TextUtils.isEmpty(guid))
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                activatedConfig.clear();
                String content = Utils.readFromFile(context, getActivatedFullPath());
                //apply default config first
                if (defaultConfig != null && !defaultConfig.isEmpty()) {
                    activatedConfig.putAll(defaultConfig);
                }
                if (!TextUtils.isEmpty(content)) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        Iterator<String> iterator = jsonObject.keys();
                        while (iterator.hasNext()) {

                            String key = iterator.next();
                            if (!TextUtils.isEmpty(key)) {
                                String value = String.valueOf(jsonObject.get(key));
                                activatedConfig.put(key, value);
                            }
                        }
                        isInitialized = true;
                        Utils.writeJsonToFile(context, getActivatedDirName(), getActivatedFileName(), new JSONObject(activatedConfig));
                        initSharedPrefValues();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isInitSuccess) {
                if (isInitSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Init Success");
                    listener.onInitSuccess();
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Init Failed");
                    listener.onInitFailed();
                }
            }
        });
    }

    private void initSharedPrefValues() {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, CTProductConfigConstants.PRODUCT_CONFIG_PREF);
        arpValues[0] = prefs.getInt(Constants.PRODUCT_CONFIG_NO_OF_CALLS, CTProductConfigConstants.DEFAULT_NO_OF_CALLS);
        arpValues[1] = prefs.getInt(Constants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, CTProductConfigConstants.DEFAULT_WINDOW_LENGTH_MINS);

        lastFetchTimeStampInMillis = prefs.getLong(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP, 0);
    }

    public void setDefaults(int resourceID) {
        defaultConfig = DefaultXmlParser.getDefaultsFromXml(context, resourceID);
        initAsync();
    }

    public void fetch() {
        fetch(minFetchIntervalInSeconds);
    }

    public void fetch(long minimumFetchIntervalInSeconds) {
        if (canRequest()) {
            isFetching = true;
            listener.fetchProductConfig();
        } else {
            config.getLogger().verbose(config.getAccountId(), "Product Config: Throttled");
        }
    }

    public void activate() {
        if (isActivating)
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                isActivating = true;
                activatedConfig.clear();
                //apply default config first
                if (defaultConfig != null && !defaultConfig.isEmpty()) {
                    activatedConfig.putAll(defaultConfig);
                }
                //read fetched info
                String content = Utils.readFromFile(context, getFetchedFullPath());
                if (!TextUtils.isEmpty(content)) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        JSONArray kvArray = jsonObject.getJSONArray(Constants.KEY_KV);

                        if (kvArray != null && kvArray.length() > 0) {
                            for (int i = 0; i < kvArray.length(); i++) {
                                JSONObject object = (JSONObject) kvArray.get(i);
                                if (object != null) {
                                    String Key = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY);
                                    String Value = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE);
                                    if (!TextUtils.isEmpty(Key)) {
                                        activatedConfig.put(Key, String.valueOf(Value));
                                    }
                                }
                            }
                        }

                        Utils.writeJsonToFile(context, getActivatedDirName(), getActivatedFileName(), new JSONObject(activatedConfig));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Activate Success");
                    listener.onActivateSuccess();
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Activate Failed");
                    listener.onActivateFailed();
                }
                isActivating = false;
            }
        });
    }

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        this.minFetchIntervalInSeconds = fetchIntervalInSeconds;
    }

    public String getString(String Key) {
        return activatedConfig.get(Key);
    }

    public boolean getBoolean(String Key) {
        return Boolean.parseBoolean(activatedConfig.get(Key));
    }

    private boolean canRequest() {
        if (isFetching || TextUtils.isEmpty(guid))
            return false;
        if (arpValues[0] > 0 && arpValues[1] > 0) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastFetchTimeStampInMillis;
            return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastRequest) > (arpValues[1] / arpValues[0]);
        }
        return true;
    }

    public void afterFetchProductConfig(JSONObject kvResponse) {
        if (kvResponse != null) {
            try {
                parseLastFetchTimeStamp(kvResponse);
                Utils.writeJsonToFile(context, getFetchedDirName(), getFetchedFileName(), kvResponse);
                Utils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        config.getLogger().verbose(config.getAccountId(), "Product Config: fetch Success");
                        listener.onFetchSuccess();
                    }
                });

            } catch (Exception e) {
                config.getLogger().verbose(config.getAccountId(), "Product Config: fetch Failed");
                listener.onFetchFailed();
                e.printStackTrace();
            }
        }
        isFetching = false;
    }

    private void parseLastFetchTimeStamp(JSONObject jsonObject) throws JSONException {
        Integer timestampInSeconds = (Integer) jsonObject.get(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP);
        if (timestampInSeconds > 0) {
            lastFetchTimeStampInMillis = timestampInSeconds * 1000L;
            final SharedPreferences prefs = StorageHelper.getPreferences(context, CTProductConfigConstants.PRODUCT_CONFIG_PREF);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP, lastFetchTimeStampInMillis);
            editor.commit();
        }
    }

    public void setGuidAndInit(String cleverTapID) {
        if (TextUtils.isEmpty(guid))
            return;
        this.guid = cleverTapID;
        initAsync();
    }

    private String getActivatedFileName() {
        return CTProductConfigConstants.FILE_NAME_ACTIVATED;
    }

    private String getActivatedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getFetchedDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }


    private String getFetchedFileName() {
        return CTProductConfigConstants.FILE_NAME_FETCHED;
    }

    private String getFetchedFullPath() {
        return getFetchedDirName() + "/" + getFetchedFileName();
    }

    private String getActivatedFullPath() {
        return getActivatedDirName() + "/" + getActivatedFileName();
    }

    public void setArpValue(String key, int value) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, CTProductConfigConstants.PRODUCT_CONFIG_PREF);
        final SharedPreferences.Editor editor = prefs.edit();
        switch (key) {
            case Constants.PRODUCT_CONFIG_NO_OF_CALLS:
                arpValues[0] = value;
                break;
            case Constants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS:
                arpValues[1] = value;
                break;
        }
        editor.putInt(key, value);
        StorageHelper.persist(editor);
    }

    public interface Listener {
        void onInitSuccess();

        void onInitFailed();

        void fetchProductConfig();

        void onFetchSuccess();

        void onFetchFailed();

        void onActivateSuccess();

        void onActivateFailed();
    }
}