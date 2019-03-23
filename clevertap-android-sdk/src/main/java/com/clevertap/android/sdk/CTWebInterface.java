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

    private WeakReference<CleverTapAPI> weakReference;

    public CTWebInterface(Context context, CleverTapInstanceConfig config){
        CleverTapAPI cleverTapAPI;
        if(config != null){
            cleverTapAPI = CleverTapAPI.instanceWithConfig(context,config);
        }else{
            cleverTapAPI = CleverTapAPI.getDefaultInstance(context);
        }
        this.weakReference = new WeakReference<>(cleverTapAPI);
    }


    /**
     * Method to be called from WebView Javascript to raise event in CleverTap
     * @param eventName String value of event name
     */
    @JavascriptInterface
    public void pushEvent(String eventName){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushEvent(eventName);
        }
    }

    /**
     * Method to be called from WebView Javascript to raise event with event properties in CleverTap
     * @param eventName String value of event name
     * @param eventActions Stringified JSON Object of event properties
     */
    @JavascriptInterface
    public void pushEvent(String eventName, String eventActions){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(eventActions!=null){
                try {
                    JSONObject eventActionsObject = new JSONObject(eventActions);
                    cleverTapAPI.pushEvent(eventName,Utils.convertJSONObjectToHashMap(eventActionsObject));
                } catch (JSONException e) {
                    Logger.v("Unable to parse eventActions from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v("eventActions passed to CTWebInterface is null");
            }
        }


    }

    /**
     * Method to be called from WebView Javascript to push profile properties in CleverTap
     * @param profile Stringified JSON Object of profile properties
     */
    @JavascriptInterface
    public void pushProfile(String profile){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if (profile != null) {
                try {
                    JSONObject profileObject = new JSONObject(profile);
                    cleverTapAPI.pushProfile(Utils.convertJSONObjectToHashMap(profileObject));
                } catch (JSONException e) {
                    Logger.v("Unable to parse profile from WebView " + e.getLocalizedMessage());
                }
            } else {
                Logger.v("profile passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     * @param key String value of profile property key
     * @param value String value of profile property value
     */
    @JavascriptInterface
    public void addMultiValueForKey(String key, String value){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.addMultiValueForKey(key,value);
        }
    }

    /**
     * Method to be called from WebView Javascript to add profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void addMultiValuesForKey(String key, String values){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.addMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v("Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v("values passed to CTWebInterface is null");
            }
        }

    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     * @param key String value of profile property key
     * @param value String value of profile property value
     */
    @JavascriptInterface
    public void removeMultiValueForKey(String key, String value){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if(value == null) {
                Logger.v("Value passed to CTWebInterface is null");
                return;
            }
            cleverTapAPI.removeMultiValueForKey(key,value);
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void removeMultiValuesForKey(String key, String values){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.removeMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v("Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v("values passed to CTWebInterface is null");
            }
        }
    }

    /**
     * Method to be called from WebView Javascript to remove profile properties for given key in CleverTap
     * @param key String value of profile property key
     */
    @JavascriptInterface
    public void removeValueForKey(String key){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            cleverTapAPI.removeValueForKey(key);
        }
    }

    /**
     * Method to be called from WebView Javascript to set profile properties in CleverTap
     * @param key String value of profile property key
     * @param values Stringified JSON Array of profile property values
     */
    @JavascriptInterface
    public void setMultiValueForKey(String key, String values){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            if(key == null) {
                Logger.v("Key passed to CTWebInterface is null");
                return;
            }
            if(values != null) {
                try{
                    JSONArray valuesArray = new JSONArray(values);
                    cleverTapAPI.setMultiValuesForKey(key, Utils.convertJSONArrayToArrayList(valuesArray));
                }catch (JSONException e){
                    Logger.v("Unable to parse values from WebView "+e.getLocalizedMessage());
                }
            }else{
                Logger.v("values passed to CTWebInterface is null");
            }
        }
    }
}
