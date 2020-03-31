package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.FileUtils;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_MIN_FETCH_INTERVAL_SECONDS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_NO_OF_CALLS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_STRING;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_WINDOW_LENGTH_MINS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

public class CTProductConfigController {

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
    private long minFetchIntervalInSeconds = DEFAULT_MIN_FETCH_INTERVAL_SECONDS;
    private long lastFetchTimeStampInMillis;
    private boolean isFetching = false;
    private boolean isActivating = false;
    private boolean isFetchAndActivating = false;
    private int noOfCallsInAllowedWindow = DEFAULT_NO_OF_CALLS;
    private int windowIntervalInMins = DEFAULT_WINDOW_LENGTH_MINS;

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
                try {
                    activatedConfig.clear();
                    String content = FileUtils.readFromFile(context, getActivatedFullPath());
                    //apply default config first
                    if (defaultConfig != null && !defaultConfig.isEmpty()) {
                        activatedConfig.putAll(defaultConfig);
                    }
                    if (!TextUtils.isEmpty(content)) {

                        JSONObject jsonObject = new JSONObject(content);
                        Iterator<String> iterator = jsonObject.keys();
                        while (iterator.hasNext()) {

                            String key = iterator.next();
                            if (!TextUtils.isEmpty(key)) {
                                String value = String.valueOf(jsonObject.get(key));
                                activatedConfig.put(key, value);
                            }
                        }
                        FileUtils.writeJsonToFile(context, getProductConfigDirName(), getActivatedFileName(), new JSONObject(activatedConfig));
                        initSharedPrefValues();
                        isInitialized = true;

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isInitSuccess) {
                if (isInitSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Init Success");
                    sendCallback(PROCESSING_STATE.INIT_SUCCESS);
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Init Failed");
                    sendCallback(PROCESSING_STATE.INIT_FAILED);
                }
            }
        });
    }

    private void initSharedPrefValues() {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, CTProductConfigConstants.PRODUCT_CONFIG_PREF);
        noOfCallsInAllowedWindow = prefs.getInt(Constants.PRODUCT_CONFIG_NO_OF_CALLS, DEFAULT_NO_OF_CALLS);
        windowIntervalInMins = prefs.getInt(Constants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, DEFAULT_WINDOW_LENGTH_MINS);

        lastFetchTimeStampInMillis = prefs.getLong(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP, 0);
    }

    public void setDefaults(int resourceID) {
        defaultConfig = DefaultXmlParser.getDefaultsFromXml(context, resourceID);
        initAsync();
    }

    /**
     * Starts fetching configs, adhering to the default minimum fetch interval.
     */
    public void fetch() {
        fetch(minFetchIntervalInSeconds);
    }

    /**
     * Starts fetching configs, adhering to the specified minimum fetch interval.
     *
     * @param interval
     */
    public void fetch(long interval) {
        if (canRequest()) {
            isFetching = true;
            listener.fetchProductConfig();
        } else {
            config.getLogger().verbose(config.getAccountId(), "Product Config: Throttled");
        }
    }

    /**
     * Asynchronously activates the most recently fetched configs, so that the fetched key value pairs take effect.
     */
    public void activate() {
        if (isActivating)
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                isActivating = true;
                try {
                    activatedConfig.clear();
                    //apply default config first
                    if (defaultConfig != null && !defaultConfig.isEmpty()) {
                        activatedConfig.putAll(defaultConfig);
                    }
                    //read fetched info
                    //TODO optimise
                    String content = FileUtils.readFromFile(context, getFetchedFullPath());
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
                                        activatedConfig.put(Key, String.valueOf(Value));
                                    }
                                }
                            }
                        }

                        FileUtils.writeJsonToFile(context, getProductConfigDirName(), getActivatedFileName(), new JSONObject(activatedConfig));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Activate Success");
                    sendCallback(PROCESSING_STATE.ACTIVATE_SUCCESS);
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Product Config: Activate Failed");
                    sendCallback(PROCESSING_STATE.ACTIVATE_FAILED);
                }
                isActivating = false;
                isFetchAndActivating = false;
            }
        });
    }

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        this.minFetchIntervalInSeconds = fetchIntervalInSeconds;
    }


    /**
     * Returns the parameter value for the given key as a String.
     *
     * @param Key - String
     * @return String
     */
    public String getString(String Key) {
        if (isInitialized)
            return activatedConfig.get(Key);
        return DEFAULT_VALUE_FOR_STRING;
    }

    /**
     * Returns the parameter value for the given key as a boolean.
     *
     * @param Key - String
     * @return String
     */
    public boolean getBoolean(String Key) {
        return Boolean.parseBoolean(activatedConfig.get(Key));
    }

    /**
     * Returns the parameter value for the given key as a long.
     *
     * @param Key - String
     * @return String
     */
    public long getLong(String Key) {
        if (isInitialized) {
            try {
                return Long.parseLong(activatedConfig.get(Key));
            } catch (NumberFormatException e) {
                config.getLogger().verbose(config.getAccountId(), "Error getting Long for Key-" + Key + " " + e.getMessage());
            }
        }
        return CTProductConfigConstants.DEFAULT_VALUE_FOR_LONG;
    }

    /**
     * Returns the parameter value for the given key as a double.
     *
     * @param Key String
     * @return String
     */
    public double getDouble(String Key) {
        if (isInitialized) {
            try {
                return Double.parseDouble(activatedConfig.get(Key));
            } catch (NumberFormatException e) {
                config.getLogger().verbose(config.getAccountId(), "Error getting Double for Key-" + Key + " " + e.getMessage());
            }

        }
        return CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE;
    }

    private boolean canRequest() {
        if (!isFetching && !TextUtils.isEmpty(guid)) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastFetchTimeStampInMillis;
            if (noOfCallsInAllowedWindow > 0 && windowIntervalInMins > 0) {

                return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastRequest) > (windowIntervalInMins / noOfCallsInAllowedWindow);
            }
        }
        return false;
    }

    public void afterFetchProductConfig(JSONObject kvResponse) {
        if (kvResponse != null) {
            try {
                parseLastFetchTimeStamp(kvResponse);
                FileUtils.writeJsonToFile(context, getProductConfigDirName(), getFetchedFileName(), kvResponse);
                Utils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        config.getLogger().verbose(config.getAccountId(), "Product Config: fetch Success");
                        sendCallback(PROCESSING_STATE.FETCH_SUCCESS);
                    }
                });
                if (isFetchAndActivating) {
                    activate();
                }

            } catch (Exception e) {
                config.getLogger().verbose(config.getAccountId(), "Product Config: fetch Failed");
                sendCallback(PROCESSING_STATE.FETCH_FAILED);
                e.printStackTrace();
                isFetchAndActivating = false;// set fetchAndActivating flag to false if fetch fails.
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

    private String getProductConfigDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getFetchedFileName() {
        return CTProductConfigConstants.FILE_NAME_FETCHED;
    }

    private String getFetchedFullPath() {
        return getProductConfigDirName() + "/" + getFetchedFileName();
    }

    private String getActivatedFullPath() {
        return getProductConfigDirName() + "/" + getActivatedFileName();
    }

    public void setArpValue(String key, int value) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, CTProductConfigConstants.PRODUCT_CONFIG_PREF);
        final SharedPreferences.Editor editor = prefs.edit();
        switch (key) {
            case Constants.PRODUCT_CONFIG_NO_OF_CALLS:
                noOfCallsInAllowedWindow = value;
                break;
            case Constants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS:
                windowIntervalInMins = value;
                break;
        }
        editor.putInt(key, value);
        StorageHelper.persist(editor);
    }

    /**
     * Deletes all activated, fetched and defaults configs and resets all Product Config settings.
     */
    public void reset() {
        if (null != defaultConfig) {
            defaultConfig.clear();
        }

        activatedConfig.clear();
        try {
            FileUtils.deleteDirectory(context, getProductConfigDirName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        setMinimumFetchIntervalInSeconds(DEFAULT_MIN_FETCH_INTERVAL_SECONDS);
    }

    public void setArpValue(JSONObject arp) {
        if (arp != null) {
            final Iterator<String> keys = arp.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                try {
                    final Object o = arp.get(key);
                    if (o instanceof Number) {
                        final int update = ((Number) o).intValue();
                        if (Constants.PRODUCT_CONFIG_NO_OF_CALLS.equalsIgnoreCase(key)
                                || Constants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS.equalsIgnoreCase(key)) {
                            setArpValue(key, update);
                        }
                    }
                } catch (JSONException e) {
                    // Ignore
                }
            }
        }
    }

    private void sendCallback(PROCESSING_STATE state) {
        if (state != null) {
            switch (state) {
                case INIT_SUCCESS:
                    listener.onInitSuccess();
                    break;
                case INIT_FAILED:
                    listener.onInitFailed();
                    break;
                case FETCH_FAILED:
                    listener.onFetchFailed();
                    break;
                case FETCH_SUCCESS:
                    listener.onFetchSuccess();
                    break;
                case ACTIVATE_FAILED:
                    listener.onActivateFailed();
                    break;
                case ACTIVATE_SUCCESS:
                    listener.onActivateSuccess();
                    break;
            }
        }

    }

    /**
     * Asynchronously fetches and then activates the fetched configs.
     */
    public void fetchAndActivate() {
        if (isFetchAndActivating)
            return;
        fetch();
    }

    private enum PROCESSING_STATE {
        INIT_SUCCESS, INIT_FAILED, FETCH_SUCCESS,
        FETCH_FAILED, ACTIVATE_SUCCESS, ACTIVATE_FAILED
    }

    public interface Listener extends CTProductConfigListener {
        void fetchProductConfig();
    }
}