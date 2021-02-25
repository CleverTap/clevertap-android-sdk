package com.clevertap.android.sdk.product_config;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_BOOLEAN;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_DOUBLE;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_LONG;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_VALUE_FOR_STRING;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseAnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.OnSuccessListener;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.FileUtils;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CTProductConfigController {

    private enum PROCESSING_STATE {
        INIT,
        FETCHED,
        ACTIVATED
    }

    //use lock for synchronization for read write
    final Map<String, String> activatedConfigs = Collections.synchronizedMap(new HashMap<String, String>());

    //use lock for synchronization for read write
    final Map<String, String> defaultConfigs = Collections.synchronizedMap(new HashMap<String, String>());

    AtomicBoolean isInitialized = new AtomicBoolean(false);

    final FileUtils mFileUtils;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final AtomicBoolean isFetchAndActivating = new AtomicBoolean(false);

    private final BaseAnalyticsManager mAnalyticsManager;

    private final BaseCallbackManager mCallbackManager;

    private final CoreMetaData mCoreMetaData;

    private final ProductConfigSettings settings;

    //use lock for synchronization for read write
    private final Map<String, String> waitingTobeActivatedConfig = Collections
            .synchronizedMap(new HashMap<String, String>());

    CTProductConfigController(Context context, CleverTapInstanceConfig config,
            final BaseAnalyticsManager analyticsManager, final CoreMetaData coreMetaData,
            final BaseCallbackManager callbackManager, ProductConfigSettings productConfigSettings,
            FileUtils fileUtils) {
        this.context = context;
        this.config = config;
        mCoreMetaData = coreMetaData;
        mCallbackManager = callbackManager;
        mAnalyticsManager = analyticsManager;
        settings = productConfigSettings;
        mFileUtils = fileUtils;
        initAsync();
    }

    /**
     * Asynchronously activates the most recently fetched configs, so that the fetched key value pairs take effect.
     */
    @SuppressWarnings("WeakerAccess")
    public void activate() {
        if (TextUtils.isEmpty(settings.getGuid())) {
            return;
        }
        Task<Void> task = CTExecutorFactory.getInstance(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(final Void result) {
                sendCallback(PROCESSING_STATE.ACTIVATED);
            }
        }).call(new Callable<Void>() {
            @Override
            public Void call() {
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

                        activatedConfigs.clear();
                        //apply default config first
                        if (!defaultConfigs.isEmpty()) {
                            activatedConfigs.putAll(defaultConfigs);
                        }
                        activatedConfigs.putAll(toWriteValues);
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "Activated successfully with configs: " + activatedConfigs);
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "Activate failed: " + e.getLocalizedMessage());
                    }
                    return null;
                }
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
            fetchProductConfig();
        }
    }

    /**
     * Asynchronously fetches and then activates the fetched configs.
     */
    public void fetchAndActivate() {
        fetch();
        isFetchAndActivating.set(true);
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */

    public void fetchProductConfig() {
        JSONObject event = new JSONObject();
        JSONObject notif = new JSONObject();
        try {
            notif.put("t", Constants.FETCH_TYPE_PC);
            event.put("evtName", Constants.WZRK_FETCH);
            event.put("evtData", notif);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mAnalyticsManager.sendFetchEvent(event);
        mCoreMetaData.setProductConfigRequested(true);
        config.getLogger()
                .verbose(config.getAccountId(), Constants.LOG_TAG_PRODUCT_CONFIG + "Fetching product config");
    }

    /**
     * Returns the parameter value for the given key as a boolean.
     *
     * @param Key - String
     * @return Boolean - value of the product config,if key is not present return {@link
     * CTProductConfigConstants#DEFAULT_VALUE_FOR_BOOLEAN}
     */
    public Boolean getBoolean(String Key) {
        if (isInitialized.get() && !TextUtils.isEmpty(Key)) {
            String value;
            value = activatedConfigs.get(Key);
            if (!TextUtils.isEmpty(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        return DEFAULT_VALUE_FOR_BOOLEAN;
    }

    /**
     * Returns the parameter value for the given key as a double.
     *
     * @param Key String
     * @return Double - value of the product config,if key is not present return {@link
     * CTProductConfigConstants#DEFAULT_VALUE_FOR_DOUBLE}
     */
    public Double getDouble(String Key) {
        if (isInitialized.get() && !TextUtils.isEmpty(Key)) {
            try {
                String value;
                value = activatedConfigs.get(Key);
                if (!TextUtils.isEmpty(value)) {
                    return Double.parseDouble(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                        "Error getting Double for Key-" + Key + " " + e.getLocalizedMessage());
            }
        }
        return DEFAULT_VALUE_FOR_DOUBLE;
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
     * Returns the parameter value for the given key as a long.
     *
     * @param Key - String
     * @return Long - value of the product config,if key is not present return {@link
     * CTProductConfigConstants#DEFAULT_VALUE_FOR_LONG}
     */
    public Long getLong(String Key) {
        if (isInitialized.get() && !TextUtils.isEmpty(Key)) {
            try {
                String value;
                value = activatedConfigs.get(Key);
                if (!TextUtils.isEmpty(value)) {
                    return Long.parseLong(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                        "Error getting Long for Key-" + Key + " " + e.getLocalizedMessage());
            }
        }
        return DEFAULT_VALUE_FOR_LONG;
    }

    // -----------------------------------------------------------------------//
    // ********                        Public API                        *****//
    // -----------------------------------------------------------------------//

    /**
     * Returns the parameter value for the given key as a String.
     *
     * @param Key - String
     * @return String - value of the product config,if key is not present return {@link
     * CTProductConfigConstants#DEFAULT_VALUE_FOR_STRING}
     */
    public String getString(String Key) {
        if (isInitialized.get() && !TextUtils.isEmpty(Key)) {
            String value;
            value = activatedConfigs.get(Key);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return DEFAULT_VALUE_FOR_STRING;
    }

    public boolean isInitialized() {
        return isInitialized.get();
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void onFetchFailed() {
        isFetchAndActivating.compareAndSet(true, false);
        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Fetch Failed");
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void onFetchSuccess(JSONObject kvResponse) {
        if (TextUtils.isEmpty(settings.getGuid())) {
            return;
        }
        synchronized (this) {
            if (kvResponse != null) {
                try {
                    parseFetchedResponse(kvResponse);
                    mFileUtils.writeJsonToFile(getProductConfigDirName(),
                            CTProductConfigConstants.FILE_NAME_ACTIVATED,
                            new JSONObject(waitingTobeActivatedConfig));
                    config.getLogger()
                            .verbose(ProductConfigUtil.getLogTag(config), "Fetch file-[" + getActivatedFullPath()
                                    + "] write success: " + waitingTobeActivatedConfig);
                    Task<Void> task = CTExecutorFactory.getInstance(config).mainTask();
                    task.call(new Callable<Void>() {
                        @Override
                        public Void call() {
                            config.getLogger()
                                    .verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Success");
                            sendCallback(PROCESSING_STATE.FETCHED);
                            return null;
                        }
                    });
                    if (isFetchAndActivating.getAndSet(false)) {
                        activate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config: fetch Failed");
                    sendCallback(PROCESSING_STATE.FETCHED);
                    // set fetchAndActivating flag to false if fetch fails.
                    isFetchAndActivating.compareAndSet(true, false);
                }
            }
        }
    }

    /**
     * Deletes all activated, fetched and defaults configs as well as all Product Config settings.
     */
    public void reset() {
        defaultConfigs.clear();
        activatedConfigs.clear();
        settings.initDefaults();
        eraseStoredConfigFiles();
    }

    public void resetSettings() {
        settings.reset(mFileUtils);
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void setArpValue(JSONObject arp) {
        settings.setARPValue(arp);
    }

    /**
     * Sets default configs using an XML resource.
     *
     * @param resourceID - resource Id of the XML.
     */
    public void setDefaults(final int resourceID) {
        setDefaultsWithXmlParser(resourceID, new DefaultXmlParser());
    }

    /**
     * Sets default configs using the given HashMap.
     *
     * @param map - HashMap of the default configs
     */
    public void setDefaults(final HashMap<String, Object> map) {
        Task<Void> task = CTExecutorFactory.getInstance(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(final Void aVoid) {
                initAsync();
            }
        }).call(new Callable<Void>() {
            @Override
            public Void call() {
                synchronized (this) {
                    if (map != null && !map.isEmpty()) {

                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            if (entry != null) {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                try {
                                    if (!TextUtils.isEmpty(key) && ProductConfigUtil.isSupportedDataType(value)) {
                                        defaultConfigs.put(key, String.valueOf(value));
                                    }
                                } catch (Exception e) {
                                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                            "Product Config: setDefaults Failed for Key: " + key + " with Error: " + e
                                                    .getLocalizedMessage());
                                }
                            }
                        }
                    }
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                            "Product Config: setDefaults Completed with: " + defaultConfigs);
                    return null;
                }
            }
        });
    }

    /**
     * This method is internal to CleverTap SDK.
     * Developers should not use this method manually.
     */
    public void setGuidAndInit(String cleverTapID) {
        if (isInitialized() || TextUtils.isEmpty(cleverTapID)) {
            return;
        }
        settings.setGuid(cleverTapID);
        initAsync();
    }

    @SuppressWarnings("WeakerAccess")
    /**
     * Sets the minimum interval between successive fetch calls.
     * @param fetchIntervalInSeconds- interval in seconds.
     */

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        settings.setMinimumFetchIntervalInSeconds(fetchIntervalInSeconds);
    }

    void eraseStoredConfigFiles() {
        Task<Void> task = CTExecutorFactory.getInstance(config).ioTask();
        task.call(new Callable<Void>() {
            @Override
            public Void call() {
                synchronized (this) {
                    try {
                        String dirName = getProductConfigDirName();
                        mFileUtils.deleteDirectory(dirName);
                        config.getLogger()
                                .verbose(ProductConfigUtil.getLogTag(config), "Reset Deleted Dir: " + dirName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "Reset failed: " + e.getLocalizedMessage());
                    }

                    return null;
                }
            }
        });
    }

    String getActivatedFullPath() {
        return getProductConfigDirName() + "/" + CTProductConfigConstants.FILE_NAME_ACTIVATED;
    }

    BaseAnalyticsManager getAnalyticsManager() {
        return mAnalyticsManager;
    }

    BaseCallbackManager getCallbackManager() {
        return mCallbackManager;
    }

    // -----------------------------------------------------------------------//
    // ********                        Internal API                      *****//
    // -----------------------------------------------------------------------//

    CleverTapInstanceConfig getConfig() {
        return config;
    }

    CoreMetaData getCoreMetaData() {
        return mCoreMetaData;
    }

    String getProductConfigDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + settings.getGuid();
    }

    ProductConfigSettings getSettings() {
        return settings;
    }

    void initAsync() {
        if (TextUtils.isEmpty(settings.getGuid())) {
            return;
        }
        Task<Boolean> task = CTExecutorFactory.getInstance(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(final Boolean aVoid) {
                sendCallback(PROCESSING_STATE.INIT);
            }
        }).call(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                synchronized (this) {
                    try {
                        //apply defaults
                        if (!defaultConfigs.isEmpty()) {
                            activatedConfigs.putAll(defaultConfigs);
                        }
                        HashMap<String, String> storedConfig = getStoredValues(getActivatedFullPath());
                        if (!storedConfig.isEmpty()) {
                            waitingTobeActivatedConfig.putAll(storedConfig);
                        }
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "Loaded configs ready to be applied: " + waitingTobeActivatedConfig);
                        settings.loadSettings(mFileUtils);
                        isInitialized.set(true);

                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "InitAsync failed - " + e.getLocalizedMessage());
                        return false;
                    }
                    return true;
                }
            }
        });
    }

    boolean isFetchAndActivating() {
        return isFetchAndActivating.get();
    }

    void setDefaultsWithXmlParser(final int resourceID, @NonNull final DefaultXmlParser xmlParser) {
        Task<Void> task = CTExecutorFactory.getInstance(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(final Void aVoid) {
                initAsync();
            }
        }).call(new Callable<Void>() {
            @Override
            public Void call() {
                synchronized (this) {
                    defaultConfigs.putAll(xmlParser.getDefaultsFromXml(context, resourceID));
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                            "Product Config: setDefaults Completed with: " + defaultConfigs);
                    return null;
                }
            }
        });
    }

    private boolean canRequest(long minimumFetchIntervalInSeconds) {
        boolean validGuid = !TextUtils.isEmpty(settings.getGuid());

        if (!validGuid) {
            config.getLogger()
                    .verbose(ProductConfigUtil.getLogTag(config), "Product Config: Throttled due to empty Guid");
            return false;
        }

        long lastRequestTime = settings.getLastFetchTimeStampInMillis();

        long timeDifference = (System.currentTimeMillis() - lastRequestTime) - TimeUnit.SECONDS
                .toMillis(minimumFetchIntervalInSeconds);
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

    private HashMap<String, String> convertServerJsonToMap(JSONObject jsonObject) {
        HashMap<String, String> map = new HashMap<>();
        JSONArray kvArray;
        try {
            kvArray = jsonObject.getJSONArray(Constants.KEY_KV);
        } catch (JSONException e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                    "ConvertServerJsonToMap failed - " + e.getLocalizedMessage());
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
                            map.put(Key, Value);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                            "ConvertServerJsonToMap failed: " + e.getLocalizedMessage());
                }
            }
        }
        return map;
    }

    private HashMap<String, String> getStoredValues(String fullFilePath) {
        HashMap<String, String> map = new HashMap<>();
        String content;
        try {
            content = mFileUtils.readFromFile(fullFilePath);
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                    "GetStoredValues reading file success:[ " + fullFilePath + "]--[Content]" + content);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                    "GetStoredValues reading file failed: " + e.getLocalizedMessage());
            return map;
        }
        if (!TextUtils.isEmpty(content)) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(content);
            } catch (Exception e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                        "GetStoredValues failed due to malformed json: " + e.getLocalizedMessage());
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
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                                "GetStoredValues for key " + key + " while parsing json: " + e.getLocalizedMessage());
                        continue;
                    }
                    if (!TextUtils.isEmpty(value)) {
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    private void onActivated() {
        if (mCallbackManager.getProductConfigListener() != null) {
            mCallbackManager.getProductConfigListener().onActivated();
        }
    }

    //Event

    private void onFetched() {
        if (mCallbackManager.getProductConfigListener() != null) {
            mCallbackManager.getProductConfigListener().onFetched();
        }
    }

    private void onInit() {
        if (mCallbackManager.getProductConfigListener() != null) {
            config.getLogger().verbose(config.getAccountId(), "Product Config initialized");
            mCallbackManager.getProductConfigListener().onInit();
        }
    }

    private synchronized void parseFetchedResponse(JSONObject jsonObject) {
        HashMap<String, String> map = convertServerJsonToMap(jsonObject);
        waitingTobeActivatedConfig.clear();
        waitingTobeActivatedConfig.putAll(map);
        config.getLogger()
                .verbose(ProductConfigUtil.getLogTag(config), "Product Config: Fetched response:" + jsonObject);
        Integer timestamp = null;
        try {
            timestamp = (Integer) jsonObject.get(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config),
                    "ParseFetchedResponse failed: " + e.getLocalizedMessage());
        }
        if (timestamp != null) {
            settings.setLastFetchTimeStampInMillis(timestamp * 1000L);
        }
    }

    private void sendCallback(PROCESSING_STATE state) {
        if (state != null) {
            switch (state) {
                case INIT:
                    onInit();
                    break;
                case FETCHED:
                    onFetched();
                    break;
                case ACTIVATED:
                    onActivated();
                    break;
            }
        }
    }
}