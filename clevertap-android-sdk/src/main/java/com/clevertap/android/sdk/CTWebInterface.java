package com.clevertap.android.sdk;

import android.content.Context;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class helps WebViews to interact with CleverTapAPI via pre-defined methods
 */
public class CTWebInterface {

    interface CTWebListener {
        void pushEventFromJS(String eventName);
        void pushEventWithPropertiesFromJS(String eventName, Map<String, Object> eventActions);
        void pushProfileFromJS(Map<String,Object> profile);
        void addMultiValueForKeyFromJS(String key, String value);
        void addMultiValuesForKeyFromJS(String key, ArrayList<String> values);
        void removeMultiValueForKeyFromJS(String key, String value);
        void removeMultiValuesForKeyFromJS(String key, ArrayList<String> values);
        void removeValueForKeyFromJS(String key);
        void setMultiValuesForKeyFromJS(String key, ArrayList<String> values);
    }

    Context context;
    CleverTapInstanceConfig config;
    private WeakReference<CTWebListener> listenerWeakReference;


    public CTWebInterface(Context context, CleverTapInstanceConfig config){
        this.context = context;
        this.config = config;
        setListener(CleverTapAPI.instanceWithConfig(this.context,this.config));
    }

    private CTWebListener getListener() {
        CTWebListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
        return listener;
    }

    private void setListener(CTWebListener listener){
        listenerWeakReference = new WeakReference<>(listener);
    }

    /**
     * Method to be called from WebView Javascript to raise event in CleverTap
     * @param eventName String value of event name
     */
    @JavascriptInterface
    public void pushEvent(String eventName){
        CTWebListener listener = getListener();
        if(listener != null){
            if(eventName == null){
                Logger.v(config.getAccountId(),"Event name passed to CTWebInterface is null");
                return;
            }
            listener.pushEventFromJS(eventName);
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event with event properties in CleverTap
     * @param eventName String value of event name
     * @param eventActions Stringified JSON Object of event properties
     */
    @JavascriptInterface
    public void pushEvent(String eventName, String eventActions){
        CTWebListener listener = getListener();
        if(listener != null){
            if(eventName == null){
                Logger.v(config.getAccountId(),"Event name passed to CTWebInterface is null");
                return;
            }
            if(eventActions!=null){
                try {
                    JSONObject eventActionsObject = new JSONObject(eventActions);
                    listener.pushEventWithPropertiesFromJS(eventName,Utils.convertJSONObjectToHashMap(eventActionsObject));
                } catch (JSONException e) {
                    Logger.v(config.getAccountId(),"Unable to parse eventActions from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v(config.getAccountId(),"eventActions passed to CTWebInterface is null");
            }
        }else{
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
    }

    /**
     * Method to be called from WebView Javascript to push profile properties in CleverTap
     * @param profile Stringified JSON Object of profile properties
     */
    @JavascriptInterface
    public void pushProfile(String profile){
        CTWebListener listener = getListener();
        if(listener != null){
            if(profile!=null){
                try {
                    JSONObject profileObject = new JSONObject(profile);
                    listener.pushProfileFromJS(Utils.convertJSONObjectToHashMap(profileObject));
                } catch (JSONException e) {
                    Logger.v(config.getAccountId(),"Unable to parse profile from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v(config.getAccountId(),"profile passed to CTWebInterface is null");
            }
        }else{
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     * @param key String value of profile property key
     * @param value String value of profile property value
     */
    @JavascriptInterface
    public void addMultiValueForKey(String key, String value){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            if(value == null) {
                Logger.v(config.getAccountId(),"Value passed to CTWebInterface is null");
                return;
            }
            listener.addMultiValueForKeyFromJS(key,value);
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void addMultiValuesForKey(String key, String values){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    listener.addMultiValuesForKeyFromJS(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v(config.getAccountId(),"Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v(config.getAccountId(),"values passed to CTWebInterface is null");
            }
        }
        else{
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     * @param key String value of profile property key
     * @param value String value of profile property value
     */
    @JavascriptInterface
    public void removeMultiValueForKey(String key, String value){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            if(value == null) {
                Logger.v(config.getAccountId(),"Value passed to CTWebInterface is null");
                return;
            }
            listener.removeMultiValueForKeyFromJS(key,value);
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void removeMultiValuesForKey(String key, String values){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    listener.removeMultiValuesForKeyFromJS(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v(config.getAccountId(),"Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v(config.getAccountId(),"values passed to CTWebInterface is null");
            }
        }
        else{
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties for given key in CleverTap
     * @param key String value of profile property key
     */
    @JavascriptInterface
    public void removeValueForKey(String key){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            listener.removeValueForKeyFromJS(key);
        }
    }

    /**
     * Method to be called from WebView Javascript to set profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void setMultiValueForKey(String key, String values){
        CTWebListener listener = getListener();
        if(listener != null){
            if(key == null) {
                Logger.v(config.getAccountId(),"Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    listener.setMultiValuesForKeyFromJS(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v(config.getAccountId(),"Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v(config.getAccountId(),"values passed to CTWebInterface is null");
            }
        }
        else{
            Logger.v(config.getAccountId(),"CTWebListener is null");
        }
    }
}
