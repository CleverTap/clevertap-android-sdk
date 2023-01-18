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

import androidx.annotation.VisibleForTesting;

import com.clevertap.android.sdk.feat_variable.callbacks.CacheUpdateBlock;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Variable cache.
 *
 * @author Ansh Sachdeva.
 */
public class VarCache {
    private static final String TAG = "VarCache>";
    private static final String LEANPLUM = "__leanplum__";
    public static final String VARIABLES_KEY = "__leanplum_variables";

    private static CacheUpdateBlock updateBlock;

    @VisibleForTesting//@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final Map<String, Object> valuesFromClient = new HashMap<>();

    @VisibleForTesting
    public static final Map<String, Var<?>> vars = new ConcurrentHashMap<>();

    @VisibleForTesting
    public static final Map<String, String> defaultKinds = new HashMap<>();

    @VisibleForTesting
    public static Map<String, Object> diffs = new HashMap<>();

    @VisibleForTesting
    public static Map<String, Object> devModeValuesFromServer;

    @VisibleForTesting
    public static boolean hasReceivedDiffs = false;

    @VisibleForTesting
    public static Object merged;

    @VisibleForTesting
    public static boolean silent;


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
                valuesPtr = CTVariableUtils.traverse(valuesPtr, nameComponents[i], true);
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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false);
        }
        return (T) mergedPtr;
    }

    //will basically call applyVariableDiffs(..) with values stored in pref (after decryption) and userAttributes()
    public static void loadDiffs() {
        // if CTVariables.hasSdkError we return w/o doing anything
        if (CTVariables.hasSdkError) {return;}
        Context context = CTVariables.getContext();

        if(context==null){return;}
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        try {
            String variables = CTVariableUtils.getFromPreference(defaults, VARIABLES_KEY, "{}");
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
            merged = CTVariableUtils.mergeHelper(valuesFromClient, diffs);
        }
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
        editor.putString(VARIABLES_KEY, variablesCipher);

        CTVariableUtils.commitChanges(editor);
    }

    //will simply  set hasReceivedDiffs[g] = true; and call updateBlock[g].updateCache() which further triggers the callbacks set by user for listening to variables update
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static void triggerHasReceivedDiffs() {
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
            params.put("vars", CTVariableUtils.toJson(valuesFromClient));
            params.put("kinds", CTVariableUtils.toJson(defaultKinds));
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





    // will reset a lot of global variables
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


    /*
     * Sets whether values should be saved and callbacks triggered when the variable values get
     * updated.
     */
    public static void setSilent(boolean silent) {
        VarCache.silent = silent;
    }

    public static void setDevModeValuesFromServer(Map<String, Object> values) {
        devModeValuesFromServer = values;
    }
    public static <T> Var<T> getVariable(String name) {
        return (Var<T>) vars.get(name);
    }

    public static boolean silent() {
        return silent;
    }
    public static void setCacheUpdateBlock(CacheUpdateBlock block) {
        updateBlock = block;
    }
    public static Map<String, Object> getDiffs() {
        return diffs;
    }
    public static boolean hasReceivedDiffs() {
        return hasReceivedDiffs;
    }
}
