package com.clevertap.android.sdk.variables;

import androidx.annotation.RestrictTo;

import androidx.annotation.VisibleForTesting;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CTVariableUtils {
    public static final String VARS = "vars";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DICTIONARY = "group";
    public static final String NUMBER = "number";

    private static void log(String msg){
        Logger.d("variables", msg);
    }

    public static void updateValuesAndKinds(String name, String[] nameComponents, Object value, String kind, Map<String, Object> values, Map<String, String> kinds) {
        Object valuesPtr = values;
        if (nameComponents != null && nameComponents.length > 0) {
            for (int i = 0; i < nameComponents.length - 1; i++) {
                valuesPtr = CTVariableUtils.traverse(valuesPtr, nameComponents[i], true);// can be either the actual value, or empty hashmap
            }
            if (valuesPtr instanceof Map) {
                Map<String, Object> map = JsonUtil.uncheckedCast(valuesPtr);
                Object currentValue = map.get(nameComponents[nameComponents.length - 1]);

                if (currentValue instanceof Map && value instanceof Map) {
                    value = mergeHelper(value, currentValue);
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
     * Converts a map like following: <br>
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
    public static Map<String, Object> convertFlatMapToNestedMaps(Map<String, Object> map) {
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
                        Object currentValue = currentMap.get(component);
                        if (!(currentValue instanceof Map)) { // null or not a map
                            // If it is not a map it will fix the invalid data.
                            Map<String, Object> nestedMap = new HashMap<>();
                            currentMap.put(component, nestedMap);
                            currentMap = nestedMap;
                        } else {
                            currentMap = JsonUtil.uncheckedCast(currentMap.get(component));
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
            Map<Object, Object> castedCollection = JsonUtil.uncheckedCast(collection);
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
        if (defaultValue instanceof Integer ||
            defaultValue instanceof Long ||
            defaultValue instanceof Short ||
            defaultValue instanceof Character ||
            defaultValue instanceof Byte ||
            defaultValue instanceof BigInteger) {
            kind = NUMBER;
        }
        else if (defaultValue instanceof Float ||
                 defaultValue instanceof Double ||
                 defaultValue instanceof BigDecimal) {
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
     * Converts a map like following: <br>
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
    @VisibleForTesting
    static void convertNestedMapsToFlatMap(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                convertNestedMapsToFlatMap(prefix + key + ".", JsonUtil.uncheckedCast(value) , result);
            } else {
                result.put(prefix + key, value);
            }
        }
    }

    public static JSONObject getFlatVarsJson(Map<String, Object> values, Map<String, String> kinds) {
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
                   convertNestedMapsToFlatMap("",valueMap,flattenedValueMap);
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

    public static Map<Object, Object> deepCopyMap(Map<Object, Object> originalMap) {
        Map<Object, Object> copiedMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : originalMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                copiedMap.put(key, deepCopyMap(JsonUtil.uncheckedCast(value)));
            } else {
                copiedMap.put(key, value);
            }
        }

        return copiedMap;
    }
}
