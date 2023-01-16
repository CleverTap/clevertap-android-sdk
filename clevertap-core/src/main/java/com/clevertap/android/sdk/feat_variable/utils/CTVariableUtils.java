package com.clevertap.android.sdk.feat_variable.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.clevertap.android.sdk.feat_variable.CTVariables;
import com.clevertap.android.sdk.feat_variable.utils.Constants;

//mainly made to mock functions used Leanplum.java
public final class CTVariableUtils {

    private static final String TAG = "LPClassesMock>";


    //APIConfig.getInstance().token()
    public static String getAPIConfigToken(){ //todo : imp for vars //understand from hristo regarding how to use, if needed?
        return "todo";
    }

    //APIConfig.getInstance().getAppID()
    public static String getAPIConfigAppID(){ //todo : imp for vars //understand from hristo regarding how to use, if needed?
        return "appid";
    }

    //Leanplum.hasStarted() //todo : imp for vars //understand from hristo regarding how to use, if needed?
    public static Boolean hasStarted(){
        return true;
    }

    //LeanplumInternal.hasCalledStart()//todo : imp for vars //understand from hristo regarding how to use, if needed?
    public static Boolean hasCalledStart(){
        return true;
    }


    /* Utility functions */

    //CollectionUtil.uncheckedCast()
    @SuppressWarnings({"unchecked"})
    public static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }

    //JsonConverter.fromJson(..)
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
    private static <T> Map<String, T> mapFromJson(JSONObject object) {
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

    //JsonConverter.toJson(..)
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


    //SharedPreferenceUtil.commitChanges()
    public static void commitChanges(SharedPreferences.Editor editor){
        try {
            editor.apply();
        } catch (NoSuchMethodError e) {
            editor.commit();
        }
    }


    // LeanplumInternal.maybeThrowException(..)
    public static void maybeThrowException(RuntimeException e) {
        if (Constants.isDevelopmentModeEnabled) {
            throw e;
        } else {
            Log.e(TAG,e.getMessage() + " This error is only thrown in development mode.", e);
        }
    }



    //Log.exception(t);
    public static void exception(Throwable t){
        t.printStackTrace();
    }

    //Util.generateResourceNameFromId(resId)
    public static  String generateResourceNameFromId(int resId){
        return null;
    }
    private static int generateIdFromResourceName(String resourceName) {
        return 0;
        /*
        // Split resource name to extract folder and file name.
        String[] parts = resourceName.split("/");
        if (parts.length == 2) {
            Resources resources = Leanplum.getContext().getResources();
            // Type name represents folder where file is contained.
            String typeName = parts[0];
            String fileName = parts[1];
            String entryName = fileName;
            // Since fileName contains extension we have to remove it,
            // to be able to get resource id.
            String[] fileParts = fileName.split("\\.(?=[^\\.]+$)");
            if (fileParts.length == 2) {
                entryName = fileParts[0];
            }
            // Get identifier for a file in specified directory
            if (!TextUtils.isEmpty(typeName) && !TextUtils.isEmpty(entryName)) {
                return resources.getIdentifier(entryName, typeName, Leanplum.getContext().getPackageName());
            }
        }
        Log.d("Could not extract resource id from provided resource name: ", resourceName);
        return 0;
        */
    }



    //Leanplum.getContext()
    public static Context getContext(){
        return CTVariables.getContext();
    }

    // misc util;
    public static  void runOnMainThread(Runnable callback) {
        new Handler(Looper.getMainLooper()).post(callback);
    }

    ////FileTransferManager.getInstance().downloadFile(stringValue, urlValue, onComplete, onComplete)
    //public static void downloadFile(final String path, final String url, Runnable onResponse, Runnable onError) {}
    //
    //
    ////FileTransferManager.getInstance().sendFilesNow(fileData, filenames, streams);
    //public static void sendFilesNow(List<JSONObject> fileData, final List<String> filenames, final List<InputStream> streams) {}
    //
    ////Util.isSimulator()
    //public static boolean isSimulator() {
    //    String model = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
    //    return model.contains("google_sdk")
    //            || model.contains("emulator")
    //            || model.contains("sdk");
    //}
    //
    ////Utils.multiIndex
    //public static <T> T multiIndex(Map<?, ?> map, Object... indices) {
    //    if (map == null) {
    //        return null;
    //    }
    //    Object current = map;
    //    for (Object index : indices) {
    //        if (!((Map<?, ?>) current).containsKey(index)) {
    //            return null;
    //        }
    //        current = ((Map<?, ?>) current).get(index);
    //    }
    //    return uncheckedCast(current);
    //}
    //
    ////Leanplum.hasStartedAndRegisteredAsDeveloper()
    //public static boolean hasStartedAndRegisteredAsDeveloper() {
    //    return true; //LeanplumInternal.hasStartedAndRegisteredAsDeveloper();
    //}
    //
    ////ActionManager.getInstance().getDefinitions().getActionDefinitionMaps()
    //public static Map<String, Object> getActionDefinitionMaps(){
    //    return  new HashMap<>();
    //}


}
