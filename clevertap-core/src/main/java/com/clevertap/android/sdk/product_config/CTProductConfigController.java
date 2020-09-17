package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.FileUtils;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_BOOLEAN;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_LONG;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_STRING;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

public class CTProductConfigController {

    private String guid;
    private boolean isInitialized = false;
    private final CleverTapInstanceConfig config;
    private final Context context;
    private HashMap<String, String> defaultConfig = new HashMap<>();
    private final HashMap<String, String> activatedConfig = new HashMap<>();
    private final HashMap<String, String> waitingTobeActivatedConfig = new HashMap<>();
    private final CTProductConfigControllerListener listener;
    private boolean isFetchAndActivating = false;
    private final ProductConfigSettings settings;

    public CTProductConfigController(Context context, String guid, CleverTapInstanceConfig config, CTProductConfigControllerListener listener) {
        this.context = context;
        this.guid = guid;
        this.config = config;
        this.listener = listener;
        this.settings = new ProductConfigSettings(context, guid, config);
        initAsync();
    }

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    public boolean isInitialized() {
        return isInitialized;
    }


    /**
     * Sets default configs using an XML resource.
     *
     * @param resourceID - resource Id of the XML.
     */
    public void setDefaults(final int resourceID) {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Void>() {
            @Override
            public Void doInBackground(Void aVoid) {
                defaultConfig.putAll(DefaultXmlParser.getDefaultsFromXml(context, resourceID));
                return null;
            }

            @Override
            public void onPostExecute(Void aVoid) {
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: setDefaults Completed with: " + defaultConfig);
                initAsync();
            }
        });
    }

    /**
     * Sets default configs using the given HashMap.
     *
     * @param map - HashMap of the default configs
     */
    public void setDefaults(final HashMap<String, Object> map) {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Void>() {
            @Override
            public Void doInBackground(Void aVoid) {
                if (map != null && !map.isEmpty()) {
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        if (entry != null) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            try {
                                if (!TextUtils.isEmpty(key) && ProductConfigUtil.isSupportedDataType(value)) {
                                    defaultConfig.put(key, String.valueOf(value));
                                }
                            } catch (Exception e) {
                                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: setDefaults Failed for Key: " + key + " with Error: " + e.getLocalizedMessage());
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public void onPostExecute(Void aVoid) {
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: setDefaults Completed with: " + defaultConfig);
                initAsync();
            }
        });
    }

    /**
     * Starts fetching configs, adhering to the default minimum fetch interval.
     */
    public void fetch() {
        fetch(settings.getNextFetchIntervalInSeconds());
    }

    /**
     * Starts fetching configs, adhering to the specified minimum fetch interval in seconds.
     *
     * @param minimumFetchIntervalInSeconds - long value of seconds
     */
    @SuppressWarnings("WeakerAccess")
    public void fetch(long minimumFetchIntervalInSeconds) {
        if (canRequest(minimumFetchIntervalInSeconds)) {
            listener.fetchProductConfig();
        }
    }

    /**
     * Asynchronously activates the most recently fetched configs, so that the fetched key value pairs take effect.
     */
    @SuppressWarnings("WeakerAccess")
    public void activate() {
        if (TextUtils.isEmpty(guid))
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Void>() {
            @Override
            public Void doInBackground(Void params) {
                synchronized (this) {
                    try {
                        //read fetched info
                        HashMap<String, String> toWriteValues = new HashMap<>();
                        if (!waitingTobeActivatedConfig.isEmpty()) {
                            toWriteValues.putAll(waitingTobeActivatedConfig);
                            waitingTobeActivatedConfig.clear();
                        } else {
                            toWriteValues = getStoredValues(getActivatedFullPath());
                        }

                        activatedConfig.clear();
                        //apply default config first
                        if (defaultConfig != null && !defaultConfig.isEmpty()) {
                            activatedConfig.putAll(defaultConfig);
                        }
                        activatedConfig.putAll(toWriteValues);
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Activate failed: " + e.getLocalizedMessage());
                    }
                    return null;
                }
            }

            @Override
            public void onPostExecute(Void isSuccess) {
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Activated successfully with configs: " + activatedConfig);
                sendCallback(PROCESSING_STATE.ACTIVATED);
                isFetchAndActivating = false;
            }
        });
    }

    /**
     * Asynchronously fetches and then activates the fetched configs.
     */
    public void fetchAndActivate() {
        fetch();
        isFetchAndActivating = true;
    }

    @SuppressWarnings("WeakerAccess")
    /**
     * Sets the minimum interval between successive fetch calls.
     * @param fetchIntervalInSeconds- interval in seconds.
     */

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        settings.setMinimumFetchIntervalInSeconds(fetchIntervalInSeconds);
    }

    /**
     * Returns the last fetched timestamp in millis.
     *
     * @return - long value of timestamp in millis.
     */
    public long getLastFetchTimeStampInMillis() {
        return settings.getLastFetchTimeStampInMillis();
    }

    /**
     * Returns the parameter value for the given key as a String.
     *
     * @param Key - String
     * @return String - value of the product config,if key is not present return {@link CTProductConfigConstants#DEFAULT_VALUE_FOR_STRING}
     */
    public String getString(String Key) {
        if (isInitialized && !TextUtils.isEmpty(Key)) {
            String value = activatedConfig.get(Key);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return DEFAULT_VALUE_FOR_STRING;
    }

    /**
     * Returns the parameter value for the given key as a boolean.
     *
     * @param Key - String
     * @return Boolean - value of the product config,if key is not present return {@link CTProductConfigConstants#DEFAULT_VALUE_FOR_BOOLEAN}
     */
    public Boolean getBoolean(String Key) {
        if (isInitialized && !TextUtils.isEmpty(Key)) {
            String value = activatedConfig.get(Key);
            if (!TextUtils.isEmpty(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        return DEFAULT_VALUE_FOR_BOOLEAN;
    }

    /**
     * Returns the parameter value for the given key as a long.
     *
     * @param Key - String
     * @return Long - value of the product config,if key is not present return {@link CTProductConfigConstants#DEFAULT_VALUE_FOR_LONG}
     */
    public Long getLong(String Key) {
        if (isInitialized && !TextUtils.isEmpty(Key)) {
            try {
                String value = activatedConfig.get(Key);
                if (!TextUtils.isEmpty(value)) {
                    return Long.parseLong(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Error getting Long for Key-" + Key + " " + e.getLocalizedMessage());
            }
        }
        return DEFAULT_VALUE_FOR_LONG;
    }

    /**
     * Returns the parameter value for the given key as a double.
     *
     * @param Key String
     * @return Double - value of the product config,if key is not present return {@link CTProductConfigConstants#DEFAULT_VALUE_FOR_DOUBLE}
     */
    public Double getDouble(String Key) {
        if (isInitialized && !TextUtils.isEmpty(Key)) {
            try {
                String value = activatedConfig.get(Key);
                if (!TextUtils.isEmpty(value)) {
                    return Double.parseDouble(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Error getting Double for Key-" + Key + " " + e.getLocalizedMessage());
            }
        }
        return DEFAULT_VALUE_FOR_DOUBLE;
    }

    /**
     * Deletes all activated, fetched and defaults configs as well as all Product Config settings.
     */
    public void reset() {
        synchronized (this) {
            if (null != defaultConfig) {
                defaultConfig.clear();
            }

            activatedConfig.clear();
            TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Void>() {
                @Override
                public Void doInBackground(Void aVoid) {
                    try {
                        String dirName = getProductConfigDirName();
                        FileUtils.deleteDirectory(context, config, dirName);
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Reset Deleted Dir: " + dirName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Reset failed: " + e.getLocalizedMessage());
                    }
                    return null;
                }

                @Override
                public void onPostExecute(Void aVoid) {

                }
            });
            settings.initDefaults();
        }
    }

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    private void initAsync() {
        if (TextUtils.isEmpty(guid))
            return;
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void params) {
                synchronized (this) {
                    try {
                        //apply defaults
                        if (!defaultConfig.isEmpty()) {
                            activatedConfig.putAll(defaultConfig);
                        }
                        HashMap<String, String> storedConfig = getStoredValues(getActivatedFullPath());
                        if (!storedConfig.isEmpty()) {
                            waitingTobeActivatedConfig.putAll(storedConfig);
                        }
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Loaded configs ready to be applied: " + waitingTobeActivatedConfig);
                        settings.loadSettings();
                        isInitialized = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "InitAsync failed - " + e.getLocalizedMessage());
                        return false;
                    }
                    return true;
                }
            }

            @Override
            public void onPostExecute(Boolean isInitSuccess) {
                sendCallback(PROCESSING_STATE.INIT);
            }
        });
    }

    private HashMap<String, String> getStoredValues(String fullFilePath) {
        HashMap<String, String> map = new HashMap<>();
        String content;
        try {
            content = FileUtils.readFromFile(context, config, fullFilePath);
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetStoredValues reading file success:[ " + fullFilePath + "]--[Content]" + content);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetStoredValues reading file failed: " + e.getLocalizedMessage());
            return map;
        }
        if (!TextUtils.isEmpty(content)) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(content);
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetStoredValues failed due to malformed json: " + e.getLocalizedMessage());
                return map;
            }
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!TextUtils.isEmpty(key)) {
                    String value;
                    try {
                        value = String.valueOf(jsonObject.get(key));
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetStoredValues for key " + key + " while parsing json: " + e.getLocalizedMessage());
                        continue;
                    }
                    if (!TextUtils.isEmpty(value))
                        map.put(key, value);
                }
            }
        }
        return map;
    }

    private boolean canRequest(long minimumFetchIntervalInSeconds) {
        boolean validGuid = !TextUtils.isEmpty(guid);

        if (!validGuid) {
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: Throttled due to empty Guid");
            return false;
        }

        long lastRequestTime = settings.getLastFetchTimeStampInMillis();

        long timeDifference = (System.currentTimeMillis() - lastRequestTime) - TimeUnit.SECONDS.toMillis(minimumFetchIntervalInSeconds);
        boolean isTimeExpired = timeDifference > 0;
        if (!isTimeExpired) {
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                    "Throttled since you made frequent request- [Last Request Time-"
                            + new Date(lastRequestTime) + "], " +
                            "Try again in " + (-timeDifference / 1000L) + " seconds");
            return false;
        }
        return true;
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void onFetchFailed() {
        isFetchAndActivating = false;
        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Fetch Failed");
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void onFetchSuccess(JSONObject kvResponse) {
        if (TextUtils.isEmpty(guid))
            return;
        synchronized (this) {
            if (kvResponse != null) {
                try {
                    parseFetchedResponse(kvResponse);
                    FileUtils.writeJsonToFile(context, config, getProductConfigDirName(), CTProductConfigConstants.FILE_NAME_ACTIVATED, new JSONObject(waitingTobeActivatedConfig));
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Fetch file-[" + getActivatedFullPath()
                            + "] write success: " + waitingTobeActivatedConfig);
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Success");
                            sendCallback(PROCESSING_STATE.FETCHED);
                        }
                    });
                    if (isFetchAndActivating) {
                        activate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Failed");
                    sendCallback(PROCESSING_STATE.FETCHED);
                    isFetchAndActivating = false;// set fetchAndActivating flag to false if fetch fails.
                }
            }
        }
    }

    private void parseFetchedResponse(JSONObject jsonObject) {
        HashMap<String, String> map = convertServerJsonToMap(jsonObject);
        waitingTobeActivatedConfig.clear();
        waitingTobeActivatedConfig.putAll(map);
        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: Fetched response:" + jsonObject);
        Integer timestamp = null;
        try {
            timestamp = (Integer) jsonObject.get(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "ParseFetchedResponse failed: " + e.getLocalizedMessage());
        }
        if (timestamp != null) {
            settings.setLastFetchTimeStampInMillis(timestamp * 1000L);
        }
    }

    private HashMap<String, String> convertServerJsonToMap(JSONObject jsonObject) {
        HashMap<String, String> map = new HashMap<>();
        JSONArray kvArray;
        try {
            kvArray = jsonObject.getJSONArray(Constants.KEY_KV);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "ConvertServerJsonToMap failed - " + e.getLocalizedMessage());
            return map;
        }

        if (kvArray != null && kvArray.length() > 0) {
            for (int i = 0; i < kvArray.length(); i++) {
                JSONObject object;
                try {
                    object = (JSONObject) kvArray.get(i);
                    if (object != null) {
                        String Key = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_KEY);
                        String Value = object.getString(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE);
                        if (!TextUtils.isEmpty(Key)) {
                            map.put(Key, String.valueOf(Value));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "ConvertServerJsonToMap failed: " + e.getLocalizedMessage());
                }
            }
        }
        return map;
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void setGuidAndInit(String cleverTapID) {
        if (TextUtils.isEmpty(guid))
            return;
        this.guid = cleverTapID;
        initAsync();
    }

    private String getProductConfigDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getActivatedFullPath() {
        return getProductConfigDirName() + "/" + CTProductConfigConstants.FILE_NAME_ACTIVATED;
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void setArpValue(JSONObject arp) {
        settings.setARPValue(arp);
    }

    private void sendCallback(PROCESSING_STATE state) {
        if (state != null) {
            switch (state) {
                case INIT:
                    listener.onInit();
                    break;
                case FETCHED:
                    listener.onFetched();
                    break;
                case ACTIVATED:
                    listener.onActivated();
                    break;
            }
        }
    }

    public void resetSettings() {
        settings.reset();
    }

    private enum PROCESSING_STATE {
        INIT,
        FETCHED,
        ACTIVATED
    }
}