package com.clevertap.android.sdk.product_config;

import android.content.Context;

import java.util.HashMap;

public final class ProductConfigController {
    private final String accountId;
    private final Context context;
    private HashMap<String, String> defaultConfig;
    private final Listener listener;
    private long minFetchIntervalInSeconds;

    public ProductConfigController(Context context, String accountId, Listener listener) {
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


    public interface Listener {
        void fetchProductConfig();
    }

    private boolean canRequest() {
        //TODO throttling logic
        return true;
    }

}