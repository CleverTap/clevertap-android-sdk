package com.clevertap.android.sdk.featureFlags;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class CTFeatureFlagsController {

    private String guid;
    private CleverTapInstanceConfig config;
    private HashMap<String, Boolean> store;
    private boolean isIntialized = false;
    private WeakReference<FeatureFlagListener> listenerWeakReference;

    public CTFeatureFlagsController(String guid, CleverTapInstanceConfig config, FeatureFlagListener listener) {
        this.guid = guid;
        this.config = config;
        this.store = new HashMap<>();
        this.setListener(listener);
        //TODO
        unarchiveData(false);
    }

    private void setListener(FeatureFlagListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    private FeatureFlagListener getListener() {
        FeatureFlagListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),"CTABTestListener is null in CTABTestController" );
        }
        return listener;
    }


    public void updateFeatureFlags(JSONArray featureFlaglist){
        updateFeatureFlags(featureFlaglist,true);
    }

    private void notifyFeatureFlagUpdate(){
        FeatureFlagListener listener = getListener();
        if(listener != null){
            listener.featureFlagsDidUpdate();
        }
    }

    public void fetchFeatureFlags(){
        FeatureFlagListener listener = getListener();
        if(listener != null){
            listener.fetchFeatureFlags();
        }
    }

    private void updateFeatureFlags(JSONArray featureFlagList, boolean isNew) {
        getConfigLogger().verbose(getAccountId(),"Updating feature flags...");
        try {
            for (int i = 0; i < featureFlagList.length(); i++) {
                JSONObject ff = featureFlagList.getJSONObject(i);
                store.put(ff.getString("n"),ff.getBoolean("v"));
            }
        }catch (JSONException e){
            getConfigLogger().verbose(getAccountId(),"Error parsing Feature Flag array "+e.getLocalizedMessage());
        }

        if(isNew){
            archiveData(featureFlagList,false);
            notifyFeatureFlagUpdate();
        }
    }

    private void archiveData(JSONArray featureFlagList, boolean sync){
        //TODO @atul
    }

    private void unarchiveData(boolean sync){
        //TODO @atul
    }

    private boolean get(String key, boolean defaultValue){
        getConfigLogger().verbose(getAccountId(),"getting feature flag with key - "+ key +" and default value - "+ defaultValue);
        if(store.get(key) != null){
            return store.get(key);
        }else{
            getConfigLogger().verbose(getAccountId(),"feature flag not found, returning default value - "+ defaultValue);
            return defaultValue;
        }
    }

    private Logger getConfigLogger(){
        return config.getLogger();
    }

    private String getAccountId(){
        return config.getAccountId();
    }

    public void resetWithGuid(String guid){
        this.guid = guid;
    }

    public boolean isIntialized() {
        return isIntialized;
    }
}
