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
        initMapWithDefault();
    }

    private void initMapWithDefault() {
        settingsMap.put(PRODUCT_CONFIG_NO_OF_CALLS, String.valueOf(DEFAULT_NO_OF_CALLS));
        settingsMap.put(PRODUCT_CONFIG_WINDOW_LENGTH_MINS, String.valueOf(DEFAULT_WINDOW_LENGTH_MINS));
        settingsMap.put(KEY_LAST_FETCHED_TIMESTAMP, String.valueOf(0));
        settingsMap.put(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS, String.valueOf(DEFAULT_MIN_FETCH_INTERVAL_SECONDS));
    }

    /**
     * loads settings by reading from file. It's a sync call, please make sure to call this from a background thread
     */
    synchronized void loadSettings() {
        String content = null;
        try {
            content = FileUtils.readFromFile(context, getFullPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(content)) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(content);
            } catch (JSONException e) {

            }
            if (jsonObject != null) {
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (!TextUtils.isEmpty(key)) {
                        String value = null;
                        try {
                            value = String.valueOf(jsonObject.get(key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!TextUtils.isEmpty(value))
                            settingsMap.put(key, value);
                    }
                }
            }

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
            if (value != null)
                minInterVal = Long.parseLong(value);
        } catch (Exception ignored) {
        }
        return minInterVal;
    }

    long getLastFetchTimeStampInMillis() {
        long lastFetchedTimeStamp = 0L;
        String value = settingsMap.get(KEY_LAST_FETCHED_TIMESTAMP);
        try {
            if (value != null)
                lastFetchedTimeStamp = Long.parseLong(value);
        } catch (Exception ignored) {
        }
        return lastFetchedTimeStamp;
    }

    private int getNoOfCallsInAllowedWindow() {
        int noCallsAllowedInWindow = DEFAULT_NO_OF_CALLS;
        String value = settingsMap.get(PRODUCT_CONFIG_NO_OF_CALLS);
        try {
            if (value != null)
                noCallsAllowedInWindow = Integer.parseInt(value);
        } catch (Exception ignored) {
        }
        return noCallsAllowedInWindow;
    }

    private int getWindowIntervalInMinutes() {
        int windowIntervalInMinutes = DEFAULT_WINDOW_LENGTH_MINS;
        String value = settingsMap.get(PRODUCT_CONFIG_WINDOW_LENGTH_MINS);
        try {
            if (value != null)
                windowIntervalInMinutes = Integer.parseInt(value);
        } catch (Exception ignored) {
        }
        return windowIntervalInMinutes;
    }

    synchronized void setMinimumFetchIntervalInSeconds(long intervalInSeconds) {
        long minFetchIntervalInSeconds = getMinFetchIntervalInSeconds();
        if (minFetchIntervalInSeconds != intervalInSeconds) {
            settingsMap.put(PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS, String.valueOf(intervalInSeconds));
            updateConfigToFile();
        }
    }

    synchronized void setLastFetchTimeStampInMillis(long timeStampInMillis) {
        long lastFetchTimeStampInMillis = getLastFetchTimeStampInMillis();
        if (lastFetchTimeStampInMillis != timeStampInMillis) {
            settingsMap.put(KEY_LAST_FETCHED_TIMESTAMP, String.valueOf(timeStampInMillis));
            updateConfigToFile();
        }
    }

    synchronized void setNoOfCallsInAllowedWindow(int callsInAllowedWindow) {
        long noOfCallsInAllowedWindow = getNoOfCallsInAllowedWindow();
        if (noOfCallsInAllowedWindow != callsInAllowedWindow) {
            settingsMap.put(PRODUCT_CONFIG_NO_OF_CALLS, String.valueOf(callsInAllowedWindow));
            updateConfigToFile();
        }
    }

    synchronized void setWindowIntervalInMinutes(int intervalInMinutes) {
        int windowIntervalInMinutes = getWindowIntervalInMinutes();
        if (windowIntervalInMinutes != intervalInMinutes) {
            settingsMap.put(PRODUCT_CONFIG_WINDOW_LENGTH_MINS, String.valueOf(intervalInMinutes));
            updateConfigToFile();
        }
    }

    private synchronized void updateConfigToFile() {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void aVoid) {
                try {
                    FileUtils.writeJsonToFile(context, getDirName(), CTProductConfigConstants.FILE_NAME_CONFIG_SETTINGS, new JSONObject(settingsMap));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    config.getLogger().verbose(config.getAccountId(), "Product Config settings: writing Success");
                } else {
                    config.getLogger().verbose(config.getAccountId(), "Product Config settings: writing Failed");
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

    void setArpValue(JSONObject arp) {
        if (arp != null) {
            final Iterator<String> keys = arp.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                try {
                    final Object o = arp.get(key);
                    if (o instanceof Number) {
                        final int update = ((Number) o).intValue();
                        if (CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS.equalsIgnoreCase(key)
                                || CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS.equalsIgnoreCase(key)) {
                            setArpValue(key, update);
                        }
                    }
                } catch (JSONException e) {
                    // Ignore
                }
            }
        }
    }

    void setArpValue(String key, int value) {
        switch (key) {
            case CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS:
                setNoOfCallsInAllowedWindow(value);
                break;
            case CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS:
                setWindowIntervalInMinutes(value);
                break;
        }
    }
}