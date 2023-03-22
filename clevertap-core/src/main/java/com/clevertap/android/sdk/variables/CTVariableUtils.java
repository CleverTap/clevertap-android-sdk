package com.clevertap.android.sdk.variables;

import android.text.Editable;

import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CTVariableUtils {
    public static final String VARS = "vars";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DICTIONARY = "group";
    public static final String NUMBER = "number";

    private static void log(String msg){
        Logger.v("ctv_VARIABLEUTILS",msg);
    }

    // name: "group1.myVariable", nameComponents: ['group1','myVariable'], value: 12.4, kind: "float", values:valuesFromClient[G],kinds: defaultKinds[G]
    //-----
    // this will basically update:
    // values from mapOf() to mapOf("group1":mapOf('myvariable':12.4)) and
    // kinds from mapOf() to mapOf("group1.myVariable" : "float")
    // check test for a more clarity
    public static void updateValuesAndKinds(String name, String[] nameComponents, Object value, String kind, Map<String, Object> values, Map<String, String> kinds) {
        Object valuesPtr = values;
        if (nameComponents != null && nameComponents.length > 0) {
            for (int i = 0; i < nameComponents.length - 1; i++) {
                valuesPtr = CTVariableUtils.traverse(valuesPtr, nameComponents[i], true);// can be either the actual value, or empty hashmap
            }
            if (valuesPtr instanceof Map) {
                Map<String, Object> map = CTVariableUtils.uncheckedCast(valuesPtr);
                Object currentValue = map.get(nameComponents[nameComponents.length - 1]);

                if (currentValue instanceof Map && value instanceof Map) {
                    ((Map)value).putAll((Map)currentValue);
                } else if (currentValue != null && currentValue.equals(value)) {
                    log(String.format("Variable with name %s will override value: %s, with new value: %s.", name, currentValue, value));
                }

                map.put(nameComponents[nameComponents.length - 1], value);
            }
        }
        if (kinds != null) {
            kinds.put(name, kind);
        }
    }

    /**
     *
     *
     * converts a map like following: <br>
     * <code>
     * &emsp;[<br>
     * &emsp;&emsp;"android.samsung.s22" : 54000,<br>
     * &emsp;&emsp;"android.samsung.s23" : "unreleased",<br>
     *&emsp;&emsp;"welcomeMsg": "hello"<br>
     * &emsp;]<br>
     * </code>
     * to: <br>
     * <code>
     * &emsp;[<br>
     * &emsp;&emsp;"android": {<br>
     * &emsp;&emsp;&emsp;&emsp;"samsung: {"s22": 54000, "s23" : "unreleased"}<br>
     * &emsp;&emsp;},<br>
     * &emsp;&emsp;"welcomeMsg" : "hello"<br>
     * &emsp;]<br>
     * </code>
     * to support legacy parsing implementation <br>
     **/
    public static Map<String, Object> convertEntriesWithGroupedKeysToNestedMaps(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.contains(".")) {
                String[] components = getNameComponents(key) ;
                int namePosition = components.length - 1;
                Map<String, Object> currentMap = result;
                for (int i = 0; i < components.length; i++) {
                    String component = components[i];
                    if (i == namePosition) {
                        currentMap.put(component, entry.getValue());
                    } else {
                        if (!currentMap.containsKey(component)) {
                            Map<String, Object> nestedMap = new HashMap<>();
                            currentMap.put(component, nestedMap);
                            currentMap = nestedMap;
                        } else {
                            currentMap = uncheckedCast(currentMap.get(component)) ;
                        }
                    }
                }
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Returns the merge of vars and diff, where values in vars are replaced from values
     * in diff, i.e. calling mergeHelper with vars=[a:10, b:20] and diff=[a:15, c:25] produces a
     * merge=[a:15, b:20, c:25]. The methods works recursively when nested maps are presented.
     *
     * @param vars - Variable values as defined by user.
     * @param diff - Variable values from server.
     *
     * @return Merge of both parameters with priority of diff over vars when overriding values.
     */
    public static Object mergeHelper(Object vars, Object diff) {
        log("mergeHelper() called with: vars = [" + vars + "], diff = [" + diff + "]");
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
                log("mergeHelper() : recursive call");
                Object mergedValues = mergeHelper(varsValue, diffsValue);
                merged.put(var, mergedValues);
            }
            return merged;
        }
        return null;
    }

    // check test for more info
    public static Object traverse(Object collection, Object key, boolean autoInsert) {
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

        return null;
    }

    /**
     * Resolves the server type of variable by checking the generic type T of the default value.
     */
    public static <T> String kindFromValue(T defaultValue) {
        String kind = null;
        if (defaultValue instanceof Integer || defaultValue instanceof Long || defaultValue instanceof Short || defaultValue instanceof Character || defaultValue instanceof Byte || defaultValue instanceof BigInteger) {
            kind = NUMBER;
        }
        else if (defaultValue instanceof Float || defaultValue instanceof Double || defaultValue instanceof BigDecimal) {
            kind = NUMBER;
        }
        else if (defaultValue instanceof String) {
            kind = STRING;
        }
        else if (defaultValue instanceof Map) {
            kind = DICTIONARY;
        }
        else if (defaultValue instanceof Boolean) {
            kind = BOOLEAN;
        }
        return kind;
    }

    /**
     * converts a string of this format : "a.b.c" to  an array of strings, i.e ["a","b","c"]
     * */
    public static String[] getNameComponents(String name) {
        try {
            return name.split("\\.");
        }
        catch (Throwable t){
            t.printStackTrace();
            return new String[]{};
        }
    }


    /**
     *
     *
     * converts a map like following: <br>
     * <code>
     * &emsp;[<br>
     * &emsp;&emsp;"android": [<br><br>
     * &emsp;&emsp;&emsp;&emsp;"samsung: [<br>
     * &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;"s22": 54000, <br>
     * &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;"s23" : "unreleased"<br>
     * &emsp;&emsp;&emsp;&emsp]<br>
     * &emsp;&emsp;],<br><br>
     * &emsp;&emsp;"welcomeMsg" : "hello"<br>
     * &emsp;]<br>
     * </code> <br><br>
     * to: <br><br>
     * <code>
     * &emsp;[<br>
     * &emsp;&emsp;"android.samsung.s22" : 54000,<br>
     * &emsp;&emsp;"android.samsung.s23" : "unreleased",<br>
     * &emsp;&emsp;"welcomeMsg": "hello"<br>
     * &emsp;]<br>
     * </code>
     * -------------------------------------- <br>
     * @author Ansh Sachdeva
     */
    private static void flattenNestedMaps(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenNestedMaps(prefix + key + ".", uncheckedCast(value) , result);
            } else {
                result.put(prefix + key, value);
            }
        }
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

    public static void maybeThrowException(RuntimeException e) {
        if (CTVariables.isInDevelopmentMode()) {
            throw e;
        } else {
            Logger.v(e.getMessage() + " This error is only thrown in development mode.", e);
        }
    }



    public static JSONObject getFlattenVarsJson(Map<String, Object> values, Map<String, String> kinds) {
       try {
           JSONObject resultJson = new JSONObject();
           resultJson.put("type", Constants.variablePayloadType);

           JSONObject vars = new JSONObject();
           for (String valueKey:values.keySet()) {

               String kind = kinds.get(valueKey);
               Object value = values.get(valueKey);
               if(value instanceof Map){
                   Map<String,Object> valueMap = new HashMap<>();
                   valueMap.put(valueKey,value);
                   Map<String,Object> flattenedValueMap = new HashMap<>();
                   flattenNestedMaps("",valueMap,flattenedValueMap);
                   for (HashMap.Entry<String,Object> entry :flattenedValueMap.entrySet()) {
                       String flattenedKey = entry.getKey();
                       Object flattenedValue = entry.getValue();
                       String flattenedValueKind = kindFromValue(flattenedValue);
                       JSONObject varData = new JSONObject();
                       varData.put("type",flattenedValueKind);
                       varData.put("defaultValue",flattenedValue);
                       vars.put(flattenedKey,varData);
                   }
               }
               else {
                   JSONObject varData = new JSONObject();
                   varData.put("type",kind);
                   varData.put("defaultValue",value);
                   vars.put(valueKey,varData);
               }
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
