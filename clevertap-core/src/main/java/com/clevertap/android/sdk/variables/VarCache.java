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

package com.clevertap.android.sdk.variables;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.annotation.VisibleForTesting;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.callbacks.CacheUpdateBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

/**
 * Variable cache.
 *
 * @author Ansh Sachdeva.
 */
public class VarCache {

    private static final String LEANPLUM = "__leanplum__";//TODO:@Ansh  Can't we change to clevertap?

    public static final String VARIABLES_KEY = "__leanplum_variables";//TODO:@Ansh  Can't we change to clevertap?

    /*
     * - gets set in init() and unset in reset()
     * - its function updateCache() gets called everytime triggerHasReceivedDiffs is called
     * - its function is implemented as CTVariables::triggerVariablesChanged()
     * - so whenever updateCache() is called,triggerVariablesChanged() is called which basically triggers users' listeners
     * */
    private CacheUpdateBlock updateBlock;

    /*
     *
     * */
    @VisibleForTesting
    public final Map<String, Object> valuesFromClient = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public final Map<String, Var<?>> vars = new ConcurrentHashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public final Map<String, String> defaultKinds = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public Map<String, Object> diffs = new HashMap<>();

    /*
     *
     * */
    @VisibleForTesting
    public Object merged;


    private boolean hasReceivedDiffs = false;


    // v: Var("group1.myVariable",12.4,"float") -> unit
    //-----
    //  1. will update vars[g] from mapOf() to mapOf("group1.myVariable" to  Var(..) )
    //  2. make synchronous call to updateValues("group1.myVariable",['group1','myVariable'],12.4,"float",valuesFromClient, defaultKinds)
    //     (last 2 are global variables,they are prob passed like this  so as to make the whole function testable without relying on global variables)
    //     This call will var's data  add to both kinds[g] map and valuesFromClient[g] map
    //     for kinds[g] it will simply change from mapOf() to mapOf("group1.myVariable": "float")
    //     for valuesFromClient, it will changed from mapOf() to mapOf("group1":mapOf('myvariable':12.4))
    //    for every next value added, the internal maps of valuesFromClient will get updated accordingly
    public void registerVariable(Var<?> var) {
        vars.put(var.name(), var);
        synchronized (valuesFromClient) {
            CTVariableUtils.updateValuesAndKinds(var.name(), var.nameComponents(), var.defaultValue(), var.kind(),
                    valuesFromClient, defaultKinds);
        }
    }


    //components:["group1","myVariable"]
    //----
    //basically calls getMergedValueFromComponentArray(components,merged[g] or valuesFromClient[g]) and returns its value
    public <T> T getMergedValueFromComponentArray(Object[] components) {
        return getMergedValueFromComponentArray(components, merged != null ? merged : valuesFromClient);
    }

    //components : ["group1","myVariable"]  , values : merged[g] or valuesFromClient[g]
    // will basically set values(i.e merged[g] or valuesFromClient[g]) to mapOf("group1"to mapOf('myVariable' to 12.4))
    @VisibleForTesting
    public <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false);
        }
        return (T) mergedPtr;
    }

    //will basically call applyVariableDiffs(..) with values stored in pref
    public void loadDiffs(Context context) { //TODO:@Ansh preference access should be on a background thread
        // if CTVariables.hasSdkError we return w/o doing anything

        if (context == null) {
            return;
        }
        SharedPreferences defaults = context.getSharedPreferences(LEANPLUM, Context.MODE_PRIVATE);
        try {
            String variablesFromCache = CTVariableUtils.getFromPreference(defaults, VARIABLES_KEY, "{}");
            Logger.v("loadDiffs: variablesFromCache='" + variablesFromCache + "'");

            Map<String, Object> variablesAsMap = CTVariableUtils.fromJson(variablesFromCache);
            //todo : concerning call^
            applyVariableDiffs(variablesAsMap);

        } catch (Exception e) {
            Logger.v("Could not load variable diffs.\n" ,e);
        }
    }

    //same as loadiffs, but will also trigger one/multi time listeners
    public void loadDiffsAndTriggerHandlers(final Context context) {
        loadDiffs(context);
        triggerHasReceivedDiffs();
    }

    //same as loadiffs, but differs in 2 aspects: 1) instead of picking data from cache, it receives data as param and 2) it will also trigger one/mult time listeners
    public void updateDiffsAndTriggerHandlers(Map<String, Object> diffs, final Context context) {
        applyVariableDiffs(diffs);
        saveDiffs(context);
        triggerHasReceivedDiffs();
    }

    // saveDiffs() is opposite of loadDiffs() and will save diffs[g]  to cache
    public void saveDiffs(Context context) {//TODO:@Ansh make sure to call on bg thread
        if (context == null) {
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
    private void applyVariableDiffs(Map<String, Object> diffs) {
        Logger.v("applyVariableDiffs() called with: diffs = [" + diffs + "]");
        if (diffs != null) {
            synchronized (valuesFromClient) {
                this.diffs = diffs;
                merged = CTVariableUtils.mergeHelper(valuesFromClient, this.diffs);
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
    public void triggerHasReceivedDiffs() {
        // update block is a callback registered by CTVariables to trigger user's callback once the diffs are changed
        hasReceivedDiffs = true;
        if (updateBlock != null) {
            updateBlock.updateCache();
        }
    }

    //will force upload vars from valuesFromClient[g] and  defaultKinds[g] map to server
    public void pushVariablesToServer(Runnable callback) {
        Logger.v("pushVariablesToServer() called");
        if (CTVariables.isInDevelopmentMode()) {

            JSONObject varsJson = CTVariableUtils.getVarsJson(valuesFromClient, defaultKinds);
            Logger.v("pushVariablesToServer: sending following vars info to server:" + varsJson);

            //todo replace with server logic
            Handler handler = new Handler();
            handler.postDelayed(callback, 2000);

        }
        else {
            Logger.v("Since your app is NOT in development mode, vars data will not be sent to server");
        }
    }


    // will reset few global variables
    public void clearUserContent() {
        //devModeValuesFromServer = null;
        diffs.clear();
        merged = null;
        vars.clear();
    }


    // will reset a lot of global variables
    public void reset() {
        defaultKinds.clear();
        //devModeValuesFromServer = null;
        diffs.clear();
        hasReceivedDiffs = false;
        merged = null;
        updateBlock = null;
        vars.clear();
        valuesFromClient.clear();
    }


    //public static void setDevModeValuesFromServer(Map<String, Object> values) {
    //    devModeValuesFromServer = values;
    //}
    //public static boolean sendContentIfChanged() {..}
    public <T> Var<T> getVariable(String name) {
        return (Var<T>) vars.get(name);
    }


    public void setCacheUpdateBlock(CacheUpdateBlock block) {
        updateBlock = block;
    }

    public Map<String, Object> getDiffs() {
        return diffs;
    }

    public boolean hasReceivedDiffs() {
        return hasReceivedDiffs;
    }
}
