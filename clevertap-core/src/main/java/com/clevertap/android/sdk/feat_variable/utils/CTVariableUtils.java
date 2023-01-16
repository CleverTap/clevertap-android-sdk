package com.clevertap.android.sdk.feat_variable.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;

import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//mainly made to mock functions used in Leanplum.java . its functions should be replaced by actual ct functions or renamed and  shifted to proper classes
public final class CTVariableUtils {
    private static final String TAG = "LPClassesMock>";

    private static boolean startApiResponseReceived = true;
    private static boolean hasStartFunctionExecuted = true;


    public static Boolean hasStarted(){
        //its true if server response for "start" api is received. //todo : decide whether it is needed // darshan
        return startApiResponseReceived;
    }

    public static void setHasStarted(boolean responseReceived){ //todo : decide whether it is needed//darshan
        startApiResponseReceived = responseReceived;
    }

     //todo : decide whether it is needed //darshan
    public static Boolean hasCalledStart(){
        // its true if  start() function has finished executing //alt for LeanplumInternal.hasCalledStart()
        return hasStartFunctionExecuted;
    }

    //todo : decide whether it is needed //darshan
    public static void  setHasCalledStart(boolean hasExecuted) {hasStartFunctionExecuted = hasExecuted; }

    /* Utility functions */

    //alt for CollectionUtil.uncheckedCast()
    @SuppressWarnings({"unchecked"})
    public static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }

    //alt for: JsonConverter.fromJson(..)
    public static Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapFromJson(new JSONObject(json));
        } catch (JSONException e) {
            Log.e(TAG,"Error converting " + json + " from JSON", e);
            return null;
        }
    }
    private static JSONArray listToJsonArray(Iterable<?> list) throws JSONException {
        if (list == null) {
            return null;
        }
        JSONArray obj = new JSONArray();
        for (Object value : list) {
            if (value instanceof Map) {
                Map<String, ?> mappedValue = uncheckedCast(value);
                value = mapToJsonObject(mappedValue);
            } else if (value instanceof Iterable) {
                value = listToJsonArray((Iterable<?>) value);
            } else if (value == null) {
                value = JSONObject.NULL;
            }
            obj.put(value);
        }
        return obj;
    }
    public static <T> Map<String, T> mapFromJson(JSONObject object) {
        if (object == null) {
            return null;
        }
        Map<String, T> result = new HashMap<>();
        Iterator<?> keysIterator = object.keys();
        while (keysIterator.hasNext()) {
            String key = (String) keysIterator.next();
            Object value = object.opt(key);
            if (value == null || value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONObject) {
                value = mapFromJson((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = listFromJson((JSONArray) value);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }
            T castedValue = uncheckedCast(value);
            result.put(key, castedValue);
        }
        return result;
    }
    private static <T> List<T> listFromJson(JSONArray json) {
        if (json == null) {
            return null;
        }
        List<Object> result = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            Object value = json.opt(i);
            if (value == null || value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONObject) {
                value = mapFromJson((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = listFromJson((JSONArray) value);
            } else if (JSONObject.NULL.equals(value)) {
                value = null;
            }
            result.add(value);
        }
        return uncheckedCast(result);
    }
    private static JSONObject mapToJsonObject(Map<String, ?> map) throws JSONException {
        if (map == null) {
            return null;
        }
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, ?> mappedValue = uncheckedCast(value);
                value = mapToJsonObject(mappedValue);
            } else if (value instanceof Iterable) {
                value = listToJsonArray((Iterable<?>) value);
            } else if (value instanceof Editable) {
                value = value.toString();
            } else if (value == null) {
                value = JSONObject.NULL;
            }
            obj.put(key, value);
        }
        return obj;
    }

    //alt for : JsonConverter.toJson(..)
    public static String toJson(Map<String, ?> map) {
        if (map == null) {
            return null;
        }
        try {
            return mapToJsonObject(map).toString();
        } catch (JSONException e) {
            Log.e(TAG,"Error converting " + map + " to JSON", e);
            return null;
        }
    }


    @NonNull //alt for:  aesContext.decodePreference
    public static String getFromPreference(SharedPreferences preferences, String key, String defaultValue) {

        String text = preferences.getString(key, null);
        if (text == null) {
            return defaultValue;
        }
        return text;
    }

    //alt for: SharedPreferenceUtil.commitChanges()
    public static void commitChanges(SharedPreferences.Editor editor){
        try {
            editor.apply();
        } catch (NoSuchMethodError e) {
            editor.commit();
        }
    }


    // alt for: LeanplumInternal.maybeThrowException(..)
    public static void maybeThrowException(RuntimeException e) {
        if (Constants.isDevelopmentModeEnabled) {
            throw e;
        } else {
            Log.e(TAG,e.getMessage() + " This error is only thrown in development mode.", e);
        }
    }


    // alt for: OperationQueue.sharedInstance().addUiOperation(callback) : todo //replace with ct logic to run callbacks on ui thread
    public static  void runOnMainThread(Runnable callback) {
        new Handler(Looper.getMainLooper()).post(callback);
    }


}
