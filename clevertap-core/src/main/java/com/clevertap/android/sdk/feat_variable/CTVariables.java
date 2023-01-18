package com.clevertap.android.sdk.feat_variable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.feat_variable.callbacks.VariablesChangedCallback;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 *  {
 *    variants:{...},
 *    locals:{
 *      "smartphone" : { "android":{"s22":54000} ,
 *       "apple":{"i14pro":"unreleased"}
 *    }
 *   }
 *
 * */
public class CTVariables {
    private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers = new ArrayList<>();
    private static final ArrayList<VariablesChangedCallback> oneTimeVariablesChangedHandlers = new ArrayList<>();
    private static Context context;
    public static final  boolean isDevelopmentModeEnabled = false;
    private static boolean startApiResponseReceived = false;
    private static boolean hasStartFunctionExecuted = false;
    public static final boolean hasSdkError =false;
    public static final String VARS_FROM_CODE = "varsFromCode";
    public static final String VARS = "vars";


    //needed by varcache to create shared prefs. so need a way to pass context there
    @Nullable
    public static Context getContext() {
        if (context == null) {
            Logger.v("Your application context is not set. You should call CTVariables.setApplicationContext(this) or CTActivityHelper.enableLifecycleCallbacks(this) in your application's " + "onCreate method, or have your application extend CleverTapApplication.");
        }
        return context;
    }
    public static void setContext(Context context) {
        CTVariables.context = context;
    }


    public static Boolean hasStarted(){
        //its true if server response for "start" api is received.
        return startApiResponseReceived;
    }

    public static void setHasStarted(boolean responseReceived){
        startApiResponseReceived = responseReceived; // might not be needed
    }

    public static Boolean hasCalledStart(){
        // its true if  start() function has finished executing //alt for LeanplumInternal.hasCalledStart()
        return hasStartFunctionExecuted;
    }

    public static void  setHasCalledStart(boolean hasExecuted) {hasStartFunctionExecuted = hasExecuted; }


    /* // can be called as  CTVariables.onAppLaunched()
     * -2. <user calls CTVariables.setContext(context)>
     * -1. User Calls Parser.parse which end up calling Varcache.registerVariable which sets
           VarCache.vars to mapOf(varname -> Var("varname",..) )
           VarCache.valuesFromClient to mapOf(varNameG->varNameL->value) (check VarCache.registerVariable()) and
           VarCache.kinds to mapOf(varname->varObj.kind)
     *  0. <user calls CTVariables.init()>
     *  1. will set silent[g] = true in VarCache
     *  2. will call loadDiffs() : which further calls applyVariableDiffs(cX: map<String,Object> created from cached data )
           which does the following:
     * * 2.1. sets VarCache.diffs = cX
     * * 2.2  sets VarCache.merged = mergedMap(VarCache.valuesFromClient, VarCache.diffs)
     * * 2.3  for each var in Vache.vars,it calls var.update() which will do the following:

     * * 2.3.1 set var.value = getMergedValueFromComponentArray() : which is basically the value from VarCache.merged or VarCache.valuesFromClient(if merged is null)
     * * 2.3.2 update var's other private variables
     * * 2.3.3 optionally trigger var's individual callbacks if CTVariables.hasStarted() is true // todo can be  set to always as we are updating the global registers to always too, or maybe based on silent?

     * * 2.4 if silent is false(currently its true, so skip for now)
     * * * 2.4.1 call safeDiffs() : which saves Varcache.diffs (from 2.1) to cache
     * * * 2.4.2 call :triggerHasReceivedDiffs() : triggers users callbacks (which gets set in step 4 later)

     *  3.  sets silent[g] = false
     *  4. sets global listeners used in 2.4.2 via VarCache.setCacheUpdateBlock(..)
     *  5. call : requestVariableDataFromServer() which creates a seperate thread, it requests for data and calls handleStartResponse(resp data):
     *  5.1a.1 will call setHasStarted(true)
     *  5.1a.2 if data is not available, it will call loadDiffs, which would again do step 2(2.1-2.4) with silent=false . so it will be doing 2.4.1 and 2.4.2 (i.e call saveDiffs() and triggerHasReceivedDiffs())

     * 5.1b.1 gets variant Values (server has 2 values : 'variants' and 'locals' (my names) .
     *       locals are values received for keys from code(keys being the variable names, again from the code)
     *       BY the server.
             the variant are changed values for the same keys that were update  ON the Server)
     * 5.2b.2 calls VarCache.applyVariableDiffs(variants)->computeMergedDictionary. which means doing step 2(2.1-2.4) also since silent is still false, therefore also do 2.4.1, 2.4.2
     * 5.2b.3 if development mode is enabled, then also call VarCache.setDevModeValuesFromServer(locals) with locals which does step 6.1-6.4
     */
    public static synchronized void init(){
        checkFailSafe();
        // we first load variables from cache . and set silent so as to not update listeners. then we reset silent to false, so next time when we load from the server, we aree able to call listeners
        VarCache.setSilent(true);
        VarCache.loadDiffs();
        VarCache.setSilent(false);
        // we register an internal listener to update client's listenere whenever the load diffs updates the variables
        VarCache.setCacheUpdateBlock(CTVariables::triggerVariablesChanged);
        requestVariableDataFromServer();
    }



    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void requestVariableDataFromServer() {
        //todo: replace with code to download clevertap variable data and pass it in
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                new Handler(Looper.getMainLooper()).post(() -> {
                    JSONObject resp = new JSONObject();// assumption this is the json received from server
                    handleStartResponse(resp);
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();

    }

    private static void checkFailSafe() {
        // this is a situation where some error happened in ct sdk. so we just apply empty to all var cache
        if (hasSdkError) {
            setHasStarted(true);
            setHasCalledStart(true);
            triggerVariablesChanged();
            VarCache.applyVariableDiffs(new HashMap<>());
            // saveDiffs();
            //            triggerHasReceivedDiffs();
        }

    }

    private static void handleStartResponse(@Nullable final JSONObject response) {
        boolean jsonHasVariableData = response!=null && true; //check if response was successful, like response.data!=null //todo add logic as per backend response structure
        try {
            if (!jsonHasVariableData) {
                setHasStarted(true);
                // Load the variables that were stored on the device from the last session.
                // this will also invoke user's callback, but with values from last session/shared prefs
                //VarCache.setSilent(false); <-- this is basically implied here, right?//todo @hristo
                VarCache.loadDiffs();
            } else {
                Map<String, Object> variants = CTVariableUtils.mapFromJson(response.optJSONObject(VARS));
                VarCache.applyVariableDiffs(variants);
                //saveDiffs(); <-- this is basically implied here since silent is always false, right?//todo @hristo
                //triggerHasReceivedDiffs();<-- this is basically implied here, right?//todo @hristo
                if (isDevelopmentModeEnabled) {
                    Map<String, Object> locals = CTVariableUtils.mapFromJson(response.optJSONObject(VARS_FROM_CODE));
                    VarCache.setDevModeValuesFromServer(locals);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    private static void triggerVariablesChanged() {
        synchronized (variablesChangedHandlers) {
            for (VariablesChangedCallback callback : variablesChangedHandlers) {
                Utils.runOnUiThread(callback);
            }
        }
        synchronized (oneTimeVariablesChangedHandlers) {
            for (VariablesChangedCallback callback : oneTimeVariablesChangedHandlers) {
                Utils.runOnUiThread(callback);
            }
            oneTimeVariablesChangedHandlers.clear();
        }
    }


    //remove all vars data . used when switching profiles
    public static void clearUserContent() {
        VarCache.clearUserContent();
    }



    public static void forceContentUpdate(@NonNull Runnable callback) {
        /*
         * Forces content to update from the server. If variables have changed, the appropriate callbacks
         * will fire. Use sparingly as if the app is updated, you'll have to deal with potentially
         * inconsistent state or user experience.
         *
         * @param callback The callback to invoke when the call completes from the server. The callback
         *                 will fire regardless of whether the variables have changed. Null value is not
         *                 permitted.
         */

        // api to  fetch variables forcefully from the server. it is same endpoint as start,
        // but with differen parameters.
        // todo : replace with ct endpopoint // backend


    }


    public static void addVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.add(handler);
        }
        // why call immediately after adding a listener? is it for some bug fix? todo @hristo
        if (VarCache.hasReceivedDiffs()) {
            handler.variablesChanged();
        }
    }
    public static void removeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.remove(handler);
        }
    }
    public static void addOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) { //addOnceVariablesChangedAndNoDownloadsPendingHandler
        //todo why one time listener is like this, while multiple time listener is attached differently? //todo @hristo
        if (VarCache.hasReceivedDiffs()) { //originally: areVariablesReceived
            handler.variablesChanged();
        } else {
            synchronized (oneTimeVariablesChangedHandlers) {
                oneTimeVariablesChangedHandlers.add(handler);
            }
        }
    }
    public static void removeOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) { //removeOnceVariablesChangedAndNoDownloadsPendingHandler

        synchronized (oneTimeVariablesChangedHandlers) {
            oneTimeVariablesChangedHandlers.remove(handler);
        }
    }






}
