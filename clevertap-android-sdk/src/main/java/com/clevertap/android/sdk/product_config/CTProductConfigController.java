package com.clevertap.android.sdk.product_config;

import android.content.Context;

import org.json.JSONArray;

import java.util.HashMap;

public final class CTProductConfigController {
    private final String accountId;
    private final Context context;
    private HashMap<String, String> defaultConfig;
    private final CTProductConfigListener listener;
    private long minFetchIntervalInSeconds;

    public CTProductConfigController(Context context, String accountId, CTProductConfigListener listener) {
        this.context = context;
        this.accountId = accountId;
        this.listener = listener;
    }

    public void setDefaults(int resourceID) {
        defaultConfig = DefaultXmlParser.getDefaultsFromXml(context, resourceID);
    }

    public void fetch() {
        fetch(minFetchIntervalInSeconds);
    }

    public void fetch(long minimumFetchIntervalInSeconds) {
        if (canRequest()) {
            listener.fetchProductConfig();
        }
    }

    public void setMinimumFetchIntervalInSeconds(long fetchIntervalInSeconds) {
        this.minFetchIntervalInSeconds = fetchIntervalInSeconds;
    }

    public String getString(String Key) {
        return defaultConfig.get(Key);
    }

    public boolean getBoolean(String Key){
        return Boolean.parseBoolean(defaultConfig.get(Key));
    }

    private boolean canRequest() {
        //TODO throttling logic
        return true;
    }

    public void afterFetchProductConfig(JSONArray kvArray) {

    }
}