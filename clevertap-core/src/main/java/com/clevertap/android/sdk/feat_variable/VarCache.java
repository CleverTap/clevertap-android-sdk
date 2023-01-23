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

    /*
     * - gets set in init() and unset in reset()
     * - its function updateCache() gets called everytime triggerHasReceivedDiffs is called
     * - its function is implemented as CTVariables::triggerVariablesChanged()
     * - so whenever updateCache() is called,triggerVariablesChanged() is called which basically triggers users' listeners
     * */
    private static CacheUpdateBlock updateBlock;

    /*
     *
     * */
    @VisibleForTesting
    public static final Map<String, Object> valuesFromClient = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public static final Map<String, Var<?>> vars = new ConcurrentHashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public static final Map<String, String> defaultKinds = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public static Map<String, Object> diffs = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public static Object merged;


    // this is an additional check/ optimisation to prevent pushLocalVariablesToServer() from sending unnecessary api request for variables that already on the server
    @VisibleForTesting
    public static Map<String, Object> devModeValuesFromServer;

    private static boolean hasReceivedDiffs = false;




    // v: Var("group1.myVariable",12.4,"float") -> unit
    //-----
    //  1. will update vars[g] from mapOf() to mapOf("group1.myVariable" to  Var(..) )
    //  2. make synchronous call to updateValues("group1.myVariable",['group1','myVariable'],12.4,"float",valuesFromClient, defaultKinds)
    //     (last 2 are global variables,they are prob passed like this  so as to make the whole function testable without relying on global variables)
    //     This call will var's data  add to both kinds[g] map and valuesFromClient[g] map
    //     for kinds[g] it will simply change from mapOf() to mapOf("group1.myVariable": "float")
    //     for valuesFromClient, it will changed from mapOf() to mapOf("group1":mapOf('myvariable':12.4))
    //    for every next value added, the internal maps of valuesFromClient will get updated accordingly
    public static void registerVariable(Var<?> var) {
        vars.put(var.name(), var);
        synchronized (valuesFromClient) {
            CTVariableUtils.updateValuesAndKinds(var.name(), var.nameComponents(), var.defaultValue(), var.kind(), valuesFromClient, defaultKinds);
        }
    }



    //components:["group1","myVariable"]
    //----
    //basically calls getMergedValueFromComponentArray(components,merged[g] or valuesFromClient[g]) and returns its value
    public static <T> T getMergedValueFromComponentArray(Object[] components) {
        return getMergedValueFromComponentArray(components, merged != null ? merged : valuesFromClient);
    }

    //components : ["group1","myVariable"]  , values : merged[g] or valuesFromClient[g]
    // will basically set values(i.e merged[g] or valuesFromClient[g]) to mapOf("group1"to mapOf('myVariable' to 12.4))
    @VisibleForTesting
    public static <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false);
        }
        return (T) mergedPtr;
    }

    //will basically call applyVariableDiffs(..) with values stored in pref
    public static void loadDiffs() {
        // if CTVariables.hasSdkError we return w/o doing anything
        if (CTVariables.hasSdkError) {return;}
        Context context = CTVariables.getContext();

        if(context==null){return;}
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        try {
            String variables = CTVariableUtils.getFromPreference(defaults, VARIABLES_KEY, "{}");
            Map<String,Object> variablesAsMap = CTVariableUtils.fromJson(variables);
            applyVariableDiffs(variablesAsMap);

        } catch (Exception e) {
            Log.e(TAG,"Could not load variable diffs.\n" + Log.getStackTraceString(e));
        }
    }
    public  static  void  loadDiffsAndTriggerHandlers(){
        loadDiffs();
        triggerHasReceivedDiffs();
    }

    public  static  void  updateDiffs(Map<String, Object> diffs){
        applyVariableDiffs(diffs);
        saveDiffs();
        triggerHasReceivedDiffs();
    }

    // saveDiffs() is opposite of loadDiffs() and will save diffs[g]  to cache
    private static void saveDiffs() {
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

    //will basically
    // 1) set diffs[g] = diffs
    // (2.) call computeMergedDictionary()
    // (3.) call var.update() for every var in vars[g]
    // (4.)  if silent is false,  call saveDiffs() and triggerHasReceivedDiffs()
    private static void applyVariableDiffs(Map<String, Object> diffs) {
        if (diffs != null) {
            synchronized (valuesFromClient) {
                VarCache.diffs = diffs;
                merged = CTVariableUtils.mergeHelper(valuesFromClient, VarCache.diffs);
            }
            // Update variables with new values. Have to copy the dictionary because a
            // dictionary variable may add a new sub-variable, modifying the variable dictionary.
            for (String name : new HashMap<>(vars).keySet()) {
                Var<?> var = vars.get(name);
                if (var != null) {
                    var.update();
                }
            }
        }

    }


    //will simply  set hasReceivedDiffs[g] = true; and call updateBlock[g].updateCache() which further triggers the callbacks set by user for listening to variables update
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static void triggerHasReceivedDiffs() {
        // update block is a callback registered by CTVariables to trigger user's callback once the diffs are changed
        hasReceivedDiffs = true;
        if (updateBlock != null) {
            updateBlock.updateCache();
        }
    }

    //will force upload vars from valuesFromClient[g] and  defaultKinds[g] map to server
    //todo : can be made to use callback as a param  for when response is received
    public static void pushLocalVariablesToServer() { //originally : sendContentIfChanged()
        boolean changed = devModeValuesFromServer != null && !valuesFromClient.equals(devModeValuesFromServer);
        if (changed) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("vars", CTVariableUtils.toJson(valuesFromClient));
            params.put("kinds", CTVariableUtils.toJson(defaultKinds));
            //Request request = RequestBuilder.withSetVarsAction().andParams(params).andType(RequestType.IMMEDIATE).create();// todo : ct code to send to server
            //RequestSender.getInstance().send(request); ;// todo : ct code to send to server
        }

    }

    //public static boolean sendContentIfChanged() {..}


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
        updateBlock = null;
        vars.clear();
        valuesFromClient.clear();
    }




    public static void setDevModeValuesFromServer(Map<String, Object> values) {
        devModeValuesFromServer = values;
    }
    public static <T> Var<T> getVariable(String name) {
        return (Var<T>) vars.get(name);
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
