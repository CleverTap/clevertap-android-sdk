package com.clevertap.android.sdk.variables;

import android.content.SharedPreferences;
import android.text.Editable;

import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.variables.annotations.Variable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CTVariableUtils {
    public static final String INT = "integer";
    public static final String FLOAT = "float";
    public static final String STRING = "string";
    public static final String BOOLEAN = "bool";
    public static final String DICTIONARY = "group";
    public static final String ARRAY = "list";


    // name: "group1.myVariable", nameComponents: ['group1','myVariable'], value: 12.4, kind: "float", values:valuesFromClient[G],kinds: defaultKinds[G]
    //-----
    // this will basically update:
    // values from mapOf() to mapOf("group1":mapOf('myvariable':12.4)) and
    // kinds from mapOf() to mapOf("group1.myVariable" : "float")
    // check test for a more clarity
    public static void updateValuesAndKinds(String name, String[] nameComponents, Object value, String kind, Map<String, Object> values, Map<String, String> kinds) {
        //Logger.v( "updateValuesAndKinds() called with: name = [" + name + "], nameComponents = [" + Arrays.toString(nameComponents) + "], value = [" + value + "], kind = [" + kind + "], values = [" + values + "], kinds = [" + kinds + "]");
        Object valuesPtr = values;
        if (nameComponents != null && nameComponents.length > 0) {
            for (int i = 0; i < nameComponents.length - 1; i++) {
                valuesPtr = CTVariableUtils.traverse(valuesPtr, nameComponents[i], true);// can be either the actual value, or empty hashmap
            }
            if (valuesPtr instanceof Map) {
                Map<String, Object> map = CTVariableUtils.uncheckedCast(valuesPtr);
                map.put(nameComponents[nameComponents.length - 1], value);
            }
        }
        if (kinds != null) {
            kinds.put(name, kind);
        }
    }

    // check test for more info
    public static Object mergeHelper(Object vars, Object diff) {
        //Logger.v( "mergeHelper() called with: vars = [" + vars + "], diff = [" + diff + "]");
        if (diff == null) {
            return vars;
        }
        if (diff instanceof Number
                || diff instanceof Boolean
                || diff instanceof String
                || diff instanceof Character
                || vars instanceof Number
                || vars instanceof Boolean
                || vars instanceof String
                || vars instanceof Character) {
            return diff;
        }

        Iterable<?> diffKeys = (diff instanceof Map) ? ((Map<?, ?>) diff).keySet() : (Iterable<?>) diff;
        Iterable<?> varsKeys = (vars instanceof Map) ? ((Map<?, ?>) vars).keySet() : (Iterable<?>) vars;
        Map<?, ?> diffMap = (diff instanceof Map) ? ((Map<?, ?>) diff) : null;
        Map<?, ?> varsMap = (vars instanceof Map) ? ((Map<?, ?>) vars) : null;

        // Infer that the diffs is an array if the vars value doesn't exist to tell us the type.
        boolean isArray = false;
        if (vars == null) {
            if (diff instanceof Map && ((Map<?, ?>) diff).size() > 0) {
                isArray = true;
                for (Object var : diffKeys) {
                    if (!(var instanceof String)) {
                        isArray = false;
                        break;
                    }
                    String str = ((String) var);
                    if (str.length() < 3 || str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']') {
                        isArray = false;
                        break;
                    }
                    String varSubscript = str.substring(1, str.length() - 1);
                    if (!("" + Integer.getInteger(varSubscript)).equals(varSubscript)) {
                        isArray = false;
                        break;
                    }
                }
            }
        }

        // Merge arrays.
        if (vars instanceof List || isArray) {
            ArrayList<Object> merged = new ArrayList<>();
            if(varsKeys!=null){
                for (Object var : varsKeys) {
                    merged.add(var);
                }
            }

            // Merge values from server
            // Array values from server come as Dictionary
            // Example:
            // string[] items = new string[] { "Item 1", "Item 2"};
            // args.With<string[]>("Items", items); // Action Context arg value
            // "vars": {
            //      "Items": {
            //                  "[1]": "Item 222", // Modified value from server
            //                  "[0]": "Item 111"  // Modified value from server
            //              }
            //  }
            // Prevent error when loading variable diffs where the diff is an Array and not Dictionary
            if (diffMap != null) {
                for (Object varSubscript : diffKeys) {
                    if (varSubscript instanceof String) {
                        String strSubscript = (String) varSubscript;
                        if (strSubscript.length() > 2 && strSubscript.startsWith("[") && strSubscript.endsWith("]")) {
                            // Get the index from the string key: "[0]" -> 0
                            int subscript = Integer.parseInt(strSubscript.substring(1, strSubscript.length() - 1));
                            Object var = diffMap.get(strSubscript);
                            while (subscript >= merged.size() && merged.size() < Short.MAX_VALUE) {
                                merged.add(null);
                            }
                            merged.set(subscript, mergeHelper(merged.get(subscript), var));
                        }
                    }
                }
            }
            return merged;
        }

        // Merge dictionaries.
        if (vars instanceof Map || diff instanceof Map) {
            HashMap<Object, Object> merged = new HashMap<>();
            if (varsKeys != null) {
                for (Object var : varsKeys) {
                    if (diffMap != null && varsMap != null) {
                        Object diffVar = diffMap.get(var);
                        Object value = varsMap.get(var);
                        if (diffVar == null && value != null) {
                            merged.put(var, value);
                        }
                    }
                }
            }
            for (Object var : diffKeys) {
                Object diffsValue = diffMap != null ? diffMap.get(var) : null;
                Object varsValue = varsMap != null ? varsMap.get(var) : null;
                Object mergedValues = mergeHelper(varsValue, diffsValue);
                merged.put(var, mergedValues);
            }
            return merged;
        }
        return null;
    }

    // check test for more info
    public static Object traverse(Object collection, Object key, boolean autoInsert) {
        //Logger.v("traverse() called with: collection = [" + collection + "], key = [" + key + "], autoInsert = [" + autoInsert + "]");
        if (collection == null) {
            return null;
        }
        if (collection instanceof Map) {
            Map<Object, Object> castedCollection = CTVariableUtils.uncheckedCast(collection);
            Object result = castedCollection.get(key);
            if (autoInsert && result == null && key instanceof String) {
                result = new HashMap<String, Object>();
                castedCollection.put(key, result);
            }
            return result;
        }
        else if (collection instanceof List) {
            List<Object> castedList = CTVariableUtils.uncheckedCast(collection);
            Object result = castedList.get((Integer) key);
            if (autoInsert && result == null) {
                result = new HashMap<String, Object>();
                castedList.set((Integer) key, result);
            }
            return result;
        }

        return null;
    }

    // check test for more info
    public static <T> String kindFromValue(T defaultValue) {
        //Logger.v("kindFromValue() called with: defaultValue = [" + defaultValue + "]");
        String kind = null;
        if (defaultValue instanceof Integer || defaultValue instanceof Long || defaultValue instanceof Short || defaultValue instanceof Character || defaultValue instanceof Byte || defaultValue instanceof BigInteger) {
            kind = INT;
        }
        else if (defaultValue instanceof Float || defaultValue instanceof Double || defaultValue instanceof BigDecimal) {
            kind = FLOAT;
        }
        else if (defaultValue instanceof String) {
            kind = STRING;
        }
        else if (defaultValue instanceof List || defaultValue instanceof Array) {
            kind = ARRAY;
        }
        else if (defaultValue instanceof Map) {
            kind = DICTIONARY;
        }
        else if (defaultValue instanceof Boolean) {
            kind = BOOLEAN;
        }
        //Logger.v("kindFromValue: returns kind='"+kind+"'");
        return kind;
    }


    /**
     * Originally this was used to split  the string generated from combining {@link  Variable#group()} &
     * {@link  Variable#name()}  into array of strings, but now it returns
     * just the string as array. This is because LP server would send/consume grouped variables as <br><br>
     * <code>{"group1":{"group2":{"varname":"value"}}}</code> <br><br>
     * and CT server would send/consume group variables as<br><br>
     * <code>{"group1.group2.varname":"value"}</code>
     *
     * */
    public static String[] getNameComponents(String name) {
        return new String[]{name};
    /*
        final String NAME_COMPONENT_REGEX = "(?:[^\\.\\[.(\\\\]+|\\\\.)+";
        final Pattern NAME_COMPONENT_PATTERN = Pattern.compile(NAME_COMPONENT_REGEX);
        Matcher matcher = NAME_COMPONENT_PATTERN.matcher(name);
        List<String> components = new ArrayList<>();
        while (matcher.find()) {
            components.add(name.substring(matcher.start(), matcher.end()));
        }
        //Logger.v("getNameComponents: returns components="+components);
        return components.toArray(new String[0]);
    */
    }


    /* Utility functions */

    @SuppressWarnings({"unchecked"})
    public static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapFromJson(new JSONObject(json));
        } catch (JSONException e) {
            Logger.v("Error converting " + json + " from JSON", e);
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
    public static String toJson(Map<String, ?> map) {
        if (map == null) {
            return null;
        }
        try {
            return mapToJsonObject(map).toString();
        } catch (JSONException e) {
            Logger.v("Error converting " + map + " to JSON", e);
            return null;
        }
    }
    public static String getFromPreference(SharedPreferences preferences, String key, String defaultValue) {

        String text = preferences.getString(key, null);
        if (text == null) {
            return defaultValue;
        }
        return text;
    }
    public static void commitChanges(SharedPreferences.Editor editor){
        try {
            editor.apply();
        } catch (NoSuchMethodError e) {
            editor.commit();
        }
    }
    public static void maybeThrowException(RuntimeException e) {
        if (CTVariables.isInDevelopmentMode()) {
            throw e;
        } else {
            Logger.v(e.getMessage() + " This error is only thrown in development mode.", e);
        }
    }


    // alt for: OperationQueue.sharedInstance().addUiOperation(callback) :
    public static  void runOnUiThread(Runnable callback) {
        Utils.runOnUiThread(callback);
    }


    public static JSONObject getVarsJson(Map<String, Object> values, Map<String, String> kinds) {
        Logger.v( "getVarsJson() called with: values = [" + values + "], kinds = [" + kinds + "]");
       try {
           JSONObject resultJson = new JSONObject();
           resultJson.put("type","varsPayload");

           JSONObject vars = new JSONObject();
           for (String varname:values.keySet()) {
               /*
                  todo :need to handle map values (kind=group)
                   this code will send maps as {mapname:{type:group,value:"{x:1,y:2,...}"}}, while the
                   backend expects response as { mapname.x :{type:int,value:1} , mapname.y:{type:int,value:2} }
               */
               JSONObject varData = new JSONObject();
               varData.put("kind",kinds.get(varname));
               varData.put("value",values.get(varname));
               vars.put(varname,varData);
           }
           resultJson.put("vars",vars);
           return resultJson;
       }
       catch (Throwable t){
           t.printStackTrace();
           return new JSONObject();
       }
    }
}
