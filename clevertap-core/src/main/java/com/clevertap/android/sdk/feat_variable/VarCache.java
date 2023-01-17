/*
 * Copyright 2022, CleverTap, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.clevertap.android.sdk.feat_variable;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.clevertap.android.sdk.feat_variable.callbacks.CacheUpdateBlock;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;
import com.clevertap.android.sdk.feat_variable.utils.Constants;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable cache.
 *
 * @author Ansh Sachdeva.
 */
public class VarCache {
    private static final String TAG = "VarCache>";
    private static final String LEANPLUM = "__leanplum__";

    private static final Map<String, Object> valuesFromClient = new HashMap<>(); //originally public // variables defined in code via @Variable annotation diff
    private static final Map<String, Var<?>> vars = new ConcurrentHashMap<>();
    private static final Map<String, String> defaultKinds = new HashMap<>();
    private static Map<String, Object> diffs = new HashMap<>();
    private static Map<String, Object> devModeValuesFromServer;
    private static CacheUpdateBlock updateBlock;
    private static boolean hasReceivedDiffs = false;
    private static Object merged;
    private static boolean silent;
    private static final String NAME_COMPONENT_REGEX = "(?:[^\\.\\[.(\\\\]+|\\\\.)+";
    private static final Pattern NAME_COMPONENT_PATTERN = Pattern.compile(NAME_COMPONENT_REGEX);


    // 1.2 -> "float" , 1 -> "integer" etc
    public static <T> String kindFromValue(T defaultValue) {
        String kind = null;
        if (defaultValue instanceof Integer || defaultValue instanceof Long || defaultValue instanceof Short || defaultValue instanceof Character || defaultValue instanceof Byte || defaultValue instanceof BigInteger) {
            kind = Constants.Kinds.INT;
        }
        else if (defaultValue instanceof Float || defaultValue instanceof Double || defaultValue instanceof BigDecimal) {
            kind = Constants.Kinds.FLOAT;
        }
        else if (defaultValue instanceof String) {
            kind = Constants.Kinds.STRING;
        }
        else if (defaultValue instanceof List || defaultValue instanceof Array) {
            kind = Constants.Kinds.ARRAY;
        }
        else if (defaultValue instanceof Map) {
            kind = Constants.Kinds.DICTIONARY;
        }
        else if (defaultValue instanceof Boolean) {
            kind = Constants.Kinds.BOOLEAN;
        }
        return kind;
    }

    // "group1.group2.name" -> ["group1","group2","name"]
    public static String[] getNameComponents(String name) {

        Matcher matcher = NAME_COMPONENT_PATTERN.matcher(name);
        List<String> components = new ArrayList<>();
        while (matcher.find()) {
            components.add(name.substring(matcher.start(), matcher.end()));
        }
        return components.toArray(new String[0]);
    }


    // v: Var("group1.myVariable",12.4,"float") -> unit
    //-----
    //  1. will put v in vars
    //  2. make synchronous call to updateValues("group1.myVariable",['group1','myVariable'],12.4,"float",valuesFromClient, defaultKinds)
    //  (last 2 are global variables,they are prob passed like this  so as to make the whole function testable without relying on global variables)
    public static void registerVariable(Var<?> var) {
        vars.put(var.name(), var);
        synchronized (valuesFromClient) {
            updateValues(var.name(), var.nameComponents(), var.defaultValue(), var.kind(), valuesFromClient, defaultKinds);
        }
    }

    // name: "group1.myVariable", ameComponents: ['group1','myVariable'], value: 12.4, kind: "float", values:valuesFromClient[G],kinds: defaultKinds[G]
    //-----
    // this will basically update:
    // values(i.e valuesFromClient[G]) from mapOf() to mapOf("group1"to mapOf(),'myVariable' to mapOf()) and (kinds(i.e defaultKinds[G]) from mapOf() to mapOf("group1.myVariable" to "float")
    public static void updateValues(String name, String[] nameComponents, Object value, String kind, Map<String, Object> values, Map<String, String> kinds) {
        //if(nc=[g,m] and valuePtr = mapOf({a:b},{c:d}), then after iterating nc, valuePtr will be either b/d/emptymap based on  whether a/c=g/m .
        Object valuesPtr = values;
        if (nameComponents != null && nameComponents.length > 0) {
            for (int i = 0; i < nameComponents.length - 1; i++) {
                valuesPtr = traverse(valuesPtr, nameComponents[i], true);
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


    //components:["group1","myVariable"]
    //----
    //basically calls getMergedValueFromComponentArray(components,merged[g] or valuesFromClient[g]) and returns its value
    public static <T> T getMergedValueFromComponentArray(Object[] components) {
        // merged can be mapOf(..?..) or arraylist  . valuesFromClient can be a mapOf("group1"to mapOf(),'myVariable' to mapOf())
        return getMergedValueFromComponentArray(components, merged != null ? merged : valuesFromClient);
    }

    //components : ["group1","myVariable"]  , values : merged[g] or valuesFromClient[g]
    // will basically set values(i.e merged[g] or valuesFromClient[g]) to to mapOf("group1"to mapOf(),'myVariable' to mapOf())
    public static <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = traverse(mergedPtr, component, false);
        }
        return (T) mergedPtr;
    }




    // traverse(mapOf("key" to 1234) , "key" , true/false) ->   1234
    // traverse(mapOf("key" to 1234) , "unknownKey" , true) ->  hashMap() | also,changes collection to : mapOf("key" to 1234, "unknownKey" to hashMap())
    // traverse(mapOf("key" to 1234) , "unknownKey" , false) ->  null
    // traverse(listOf(1234,5678,1111,null),2, true/false) ->  1111
    // traverse(listOf(1234,5678,1111,null),3, true) -> hashMap() | also changes collection to : listOf(1234,5678,1111,hashMap() )
    // traverse(listOf(1234,5678,1111,null),3, false) -> null()
    //-----
    //it will either return the value of key from the collection, or empty map if key is not in
    // collection; and it will also add the empty map against that key in collection
    private static Object traverse(Object collection, Object key, boolean autoInsert) {
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



    //will basically call applyVariableDiffs(..) with values stored in pref (after decryption) and userAttributes()
    public static void loadDiffs() {
        // if CTVariables.hasSdkError we return w/o doing anything
        if (CTVariables.hasSdkError) {return;}
        Context context = CTVariables.getContext();

        if(context==null){return;}
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        try {
            String variables = CTVariableUtils.getFromPreference(defaults, Constants.Defaults.VARIABLES_KEY, "{}");
            applyVariableDiffs(CTVariableUtils.fromJson(variables));

        } catch (Exception e) {
            Log.e(TAG,"Could not load variable diffs.\n" + Log.getStackTraceString(e));
        }
    }


    //will basically 1) set diffs[g] = diffs (2.) call computeMergedDictionary() (3.) call var.update() for every var in vars[g] (4.) optionally call saveDiffs() and triggerHasReceivedDiffs()
    public static void applyVariableDiffs(Map<String, Object> diffs) {
        if (diffs != null) {
            VarCache.diffs = diffs;
            computeMergedDictionary();

            // Update variables with new values. Have to copy the dictionary because a dictionary variable may add a new sub-variable, modifying the variable dictionary.
            for (String name : new HashMap<>(vars).keySet()) {
                Var<?> var = vars.get(name);
                if (var != null) {
                    var.update();
                }
            }
        }

        // will only call saveDiffs() and triggerHasReceivedDiffs() when silent[g] is false.
        // saveDiffs() is opposite of loadDiffs() and will save the current globalVariable's data to cache
        // triggerHasReceivedDiffs() will trigger user's callbacks
        if (!silent) {
            saveDiffs();
            triggerHasReceivedDiffs();
        }
    }

    // basically  merged[g] = mergeHelper(valuesFromClient[g], diffs[g])
    private static void computeMergedDictionary() {
        synchronized (valuesFromClient) {
            merged = mergeHelper(valuesFromClient, diffs);
        }
    }

    //vars:valuesFromClient[g], diff:diffs[g]
    // this recursive function is going to return a final hashmap after merging entries with common keys among
    // valuesFromClient and diff .
    // for keys common in both vars and diff, values from diff are taken
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
            for (Object var : varsKeys) {
                merged.add(var);
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

    //save the values from global variables to shared prefs after encrypting
    public static void saveDiffs() {
        if (CTVariables.hasSdkError) {
            return;
        }
        Context context = CTVariables.getContext();
        if(context==null){
            return;
        }
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = defaults.edit();

        String variablesCipher = CTVariableUtils.toJson(diffs); // aesContext.encrypt(CTVariableUtils.toJson(diffs));
        editor.putString(Constants.Defaults.VARIABLES_KEY, variablesCipher);

        CTVariableUtils.commitChanges(editor);
    }

    //will simply  set hasReceivedDiffs[g] = true; and call updateBlock[g].updateCache() which further triggers the callbacks set by user for listening to variables update
    private static void triggerHasReceivedDiffs() {
        // update block is a callback registered by LP to trigger user's callback once the diffs are changed
        hasReceivedDiffs = true;
        if (updateBlock != null) {
            updateBlock.updateCache();
        }
    }



    //will force upload vars from vars[g] map to server
    public static boolean sendContentIfChanged() {
        boolean changed = devModeValuesFromServer != null && !valuesFromClient.equals(devModeValuesFromServer);
        if (changed) {
            HashMap<String, Object> params = new HashMap<>();
            params.put(Constants.Params.VARS, CTVariableUtils.toJson(valuesFromClient));
            params.put(Constants.Params.KINDS, CTVariableUtils.toJson(defaultKinds));
            //Request request = RequestBuilder.withSetVarsAction().andParams(params).andType(RequestType.IMMEDIATE).create();// todo : ct code to send to server
            //RequestSender.getInstance().send(request); ;// todo : ct code to send to server
        }

        return changed;
    }


    // will reset few global variables
    // and also call ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(..)
    public static void clearUserContent() {
        devModeValuesFromServer = null;
        diffs.clear();
        merged = null;
        vars.clear();
    }


    //will set devModeValuesFromServer[g], devModeFileAttributesFromServer[g]
    // and call ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(ActionManager.getInstance(), actionDefinitions)
    public static void setDevModeValuesFromServer(Map<String, Object> values) {
        devModeValuesFromServer = values;
        //ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(ActionManager.getInstance(), actionDefinitions);
        //devModeFileAttributesFromServer = fileAttributes;
    }


    // will reset a lot of global variables
    // and also call ActionManagerDefinitionKt.setDevModeActionDefinitionsFromServer(..) and ActionManager.getInstance().getDefinitions().clear();
    public static void reset() {
        defaultKinds.clear();
        devModeValuesFromServer = null;
        diffs.clear();
        hasReceivedDiffs = false;
        merged = null;
        silent = false;
        updateBlock = null;
        vars.clear();
        valuesFromClient.clear();
    }


    public static <T> Var<T> getVariable(String name) {
        return (Var<T>) vars.get(name);
    }
    public static void setSilent(boolean silent) {
        /*
         * Sets whether values should be saved and callbacks triggered when the variable values get
         * updated.
         */
        VarCache.silent = silent;
    }
    public static boolean silent() {
        return silent;
    }
    public static void onUpdate(CacheUpdateBlock block) {
        updateBlock = block;
    }
    public static Map<String, Object> getDiffs() {
        return diffs;
    }
    public static boolean hasReceivedDiffs() {
        return hasReceivedDiffs;
    }
}
