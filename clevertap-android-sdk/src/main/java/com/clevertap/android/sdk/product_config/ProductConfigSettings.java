package com.clevertap.android.sdk.product_config;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.FileUtils;
import com.clevertap.android.sdk.TaskManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_MIN_FETCH_INTERVAL_SECONDS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_NO_OF_CALLS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_WINDOW_LENGTH_MINS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS;
import static com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS;

class ProductConfigSettings {
    private final String guid;
    private final CleverTapInstanceConfig config;
    private final Context context;
    private final HashMap<String, String> settingsMap = new HashMap<>();

    ProductConfigSettings(Context context, String guid, CleverTapInstanceConfig config) {
        this.context = context.getApplicationContext();
        this.guid = guid;
        this.config = config;
        initDefaults();
    }

    void initDefaults() {
        settingsMap.put(PRODUCT_CONFIG_NO_OF_CALLS, String.valueOf(DEFAULT_NO_OF_CALLS));
        settingsMap.put(PRODUCT_CONFIG_WINDOW_LENGTH_MINS, String.valueOf(DEFAULT_WINDOW_LENGTH_MINS));
        settingsMap.put(KEY_LAST_FETCHED_TIMESTAMP, String.valueOf(0));
        settingsMap.put(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS, String.valueOf(DEFAULT_MIN_FETCH_INTERVAL_SECONDS));
        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Settings loaded with default values: " + settingsMap);
    }

    /**
     * loads settings from file.
     * It's a sync call, please make sure to call this from a background thread
     */
    synchronized void loadSettings() {
        String content;
        try {
            content = FileUtils.readFromFile(context, config, getFullPath());
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "LoadSettings failed while reading file: " + e.getLocalizedMessage());
            return;
        }
        if (!TextUtils.isEmpty(content)) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(content);
            } catch (JSONException e) {
                e.printStackTrace();
                config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "LoadSettings failed: " + e.getLocalizedMessage());
                return;
            }
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!TextUtils.isEmpty(key)) {
                    String value = null;
                    try {
                        Object obj = jsonObject.get(key);
                        if (obj != null)
                            value = String.valueOf(obj);
                    } catch (Exception e) {
                        e.printStackTrace();
                        config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Failed loading setting for key " + key + " Error: " + e.getLocalizedMessage());
                        continue;
                    }
                    if (!TextUtils.isEmpty(value))
                        settingsMap.put(key, value);
                }
            }
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "LoadSettings completed with settings: " + settingsMap);
        }
    }

    long getNextFetchIntervalInSeconds() {
        long minFetchIntervalInSecondsSDK = getMinFetchIntervalInSeconds();
        long minFetchIntervalInSecondsServer = TimeUnit.MINUTES.toSeconds(getWindowIntervalInMinutes() / getNoOfCallsInAllowedWindow());
        return Math.max(minFetchIntervalInSecondsServer, minFetchIntervalInSecondsSDK);
    }

    private long getMinFetchIntervalInSeconds() {
        long minInterVal = DEFAULT_MIN_FETCH_INTERVAL_SECONDS;
        String value = settingsMap.get(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS);
        try {
            if (!TextUtils.isEmpty(value))
                minInterVal = (long) Double.parseDouble(value);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetMinFetchIntervalInSeconds failed: " + e.getLocalizedMessage());
        }
        return minInterVal;
    }

    long getLastFetchTimeStampInMillis() {
        long lastFetchedTimeStamp = 0L;
        String value = settingsMap.get(KEY_LAST_FETCHED_TIMESTAMP);
        try {
            if (!TextUtils.isEmpty(value))
                lastFetchedTimeStamp = (long) Double.parseDouble(value);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetLastFetchTimeStampInMillis failed: " + e.getLocalizedMessage());
        }
        return lastFetchedTimeStamp;
    }

    private int getNoOfCallsInAllowedWindow() {
        int noCallsAllowedInWindow = DEFAULT_NO_OF_CALLS;
        String value = settingsMap.get(PRODUCT_CONFIG_NO_OF_CALLS);
        try {
            if (!TextUtils.isEmpty(value))
                noCallsAllowedInWindow = (int) Double.parseDouble(value);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetNoOfCallsInAllowedWindow failed: " + e.getLocalizedMessage());
        }
        return noCallsAllowedInWindow;
    }

    private int getWindowIntervalInMinutes() {
        int windowIntervalInMinutes = DEFAULT_WINDOW_LENGTH_MINS;
        String value = settingsMap.get(PRODUCT_CONFIG_WINDOW_LENGTH_MINS);
        try {
            if (!TextUtils.isEmpty(value))
                windowIntervalInMinutes = (int) Double.parseDouble(value);
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "GetWindowIntervalInMinutes failed: " + e.getLocalizedMessage());
        }
        return windowIntervalInMinutes;
    }

    synchronized void setMinimumFetchIntervalInSeconds(long intervalInSeconds) {
        long minFetchIntervalInSeconds = getMinFetchIntervalInSeconds();
        if (intervalInSeconds > 0 && minFetchIntervalInSeconds != intervalInSeconds) {
            settingsMap.put(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS, String.valueOf(intervalInSeconds));
        }
    }

    synchronized void setLastFetchTimeStampInMillis(long timeStampInMillis) {
        long lastFetchTimeStampInMillis = getLastFetchTimeStampInMillis();
        if (timeStampInMillis >= 0 && lastFetchTimeStampInMillis != timeStampInMillis) {
            settingsMap.put(KEY_LAST_FETCHED_TIMESTAMP, String.valueOf(timeStampInMillis));
            updateConfigToFile();
        }
    }

    private synchronized void setNoOfCallsInAllowedWindow(int callsInAllowedWindow) {
        long noOfCallsInAllowedWindow = getNoOfCallsInAllowedWindow();
        if (callsInAllowedWindow > 0 && noOfCallsInAllowedWindow != callsInAllowedWindow) {
            settingsMap.put(PRODUCT_CONFIG_NO_OF_CALLS, String.valueOf(callsInAllowedWindow));
            updateConfigToFile();
        }
    }

    private synchronized void setWindowIntervalInMinutes(int intervalInMinutes) {
        int windowIntervalInMinutes = getWindowIntervalInMinutes();
        if (intervalInMinutes > 0 && windowIntervalInMinutes != intervalInMinutes) {
            settingsMap.put(PRODUCT_CONFIG_WINDOW_LENGTH_MINS, String.valueOf(intervalInMinutes));
            updateConfigToFile();
        }
    }

    private synchronized void updateConfigToFile() {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void aVoid) {
                try {
                    //Ensure that we are not saving min interval in seconds
                    HashMap<String, String> toWriteMap = new HashMap<>(settingsMap);
                    toWriteMap.remove(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS);

                    FileUtils.writeJsonToFile(context, config, getDirName(), CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS, new JSONObject(toWriteMap));
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "UpdateConfigToFile failed: " + e.getLocalizedMessage());
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config settings: writing Success " + settingsMap);
                } else {
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config settings: writing Failed");
                }
            }
        });
    }

    private String getDirName() {
        return CTProductConfigConstants.DIR_PRODUCT_CONFIG + "_" + config.getAccountId() + "_" + guid;
    }

    private String getFullPath() {
        return getDirName() + "/" + CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS;
    }

    void setARPValue(JSONObject arp) {
        if (arp != null) {
            final Iterator<String> keys = arp.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                try {
                    if (!TextUtils.isEmpty(key)) {
                        final Object object = arp.get(key);
                        if (object instanceof Number) {
                            final int update = (int) ((Number) object).doubleValue();
                            if (CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS.equalsIgnoreCase(key)
                                    || CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS.equalsIgnoreCase(key)) {
                                setProductConfigValuesFromARP(key, update);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Product Config setARPValue failed " + e.getLocalizedMessage());
                }
            }
        }
    }

    private void setProductConfigValuesFromARP(String key, int value) {
        switch (key) {
            case CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS:
                setNoOfCallsInAllowedWindow(value);
                break;
            case CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS:
                setWindowIntervalInMinutes(value);
                break;
        }
    }

    public void reset() {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Void>() {
            @Override
            public Void doInBackground(Void aVoid) {
                try {
                    String fileName = getFullPath();
                    FileUtils.deleteFile(context, config, fileName);
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Deleted settings file" + fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    config.getLogger().verbose(ProductConfigUtil.getLogTag(config), "Error while resetting settings" + e.getLocalizedMessage());
                }
                return null;
            }

            @Override
            public void onPostExecute(Void aVoid) {

            }
        });
        initDefaults();
    }
}