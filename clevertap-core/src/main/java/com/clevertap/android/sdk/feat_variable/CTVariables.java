package com.clevertap.android.sdk.feat_variable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.feat_variable.callbacks.VariablesChangedCallback;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class CTVariables {
    private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers = new ArrayList<>(); //needed
    private static final ArrayList<VariablesChangedCallback> oneTimeVariablesChangedHandlers = new ArrayList<>();// needed : its actually onceVariableChanged(basically same as first in case of variables but runs only once),

    private static Context context;
    public static final  boolean isDevelopmentModeEnabled = false;


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

    static synchronized void init(){
        // this is a situation where some error happened in ct sdk. so we just apply empty to all var cache
        if (hasSdkError) {
            CTVariableUtils.setHasStarted(true);
            CTVariableUtils.setHasCalledStart(true);
            triggerVariablesChanged();
            VarCache.applyVariableDiffs(new HashMap<>());
        }
       else {
            // we first load variables from cache . and set silent so as to not update listeners. then we reset silent to false, so next time when we load from the server, we aree able to call listeners
            VarCache.setSilent(true);
            VarCache.loadDiffs();
            VarCache.setSilent(false);

            // we register an internal listener to update client's listenere whenever the load diffs updates the variables
            VarCache.onUpdate(CTVariables::triggerVariablesChanged);

            //todo: replace with code to download clevertap variable data and pass it in
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        JSONObject resp = new JSONObject()  ;// assumption this is the json received from server
                        handleStartResponse(resp);
                    });
                }catch (Throwable t){
                    t.printStackTrace();
                }

            }).start();
        }

    }

    private static void handleStartResponse(@Nullable final JSONObject response) {
        boolean jsonHasVariableData = response!=null && true; //check if response was successful, like response.data!=null //todo add logic as per backend response structure
        try {
            if (!jsonHasVariableData) {
                CTVariableUtils.setHasStarted(true);
                // Load the variables that were stored on the device from the last session.
                // this will also invoke user's callback, but with values from last session/shared prefs
                VarCache.loadDiffs();
            } else {
                Map<String, Object> values = CTVariableUtils.mapFromJson(response.optJSONObject(VARS));
                VarCache.applyVariableDiffs(values);
                if (isDevelopmentModeEnabled) {
                    Map<String, Object> valuesFromCode = CTVariableUtils.mapFromJson(response.optJSONObject(VARS_FROM_CODE));
                    VarCache.setDevModeValuesFromServer(valuesFromCode);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    private static void triggerVariablesChanged() {
        synchronized (variablesChangedHandlers) {
            for (VariablesChangedCallback callback : variablesChangedHandlers) {
                CTVariableUtils.runOnMainThread(callback);
            }
        }
        synchronized (oneTimeVariablesChangedHandlers) {
            for (VariablesChangedCallback callback : oneTimeVariablesChangedHandlers) {
                CTVariableUtils.runOnMainThread(callback);
            }
            oneTimeVariablesChangedHandlers.clear();
        }
    }



    static boolean areVariablesReceived() { //areVariablesReceivedAndNoDownloadsPending
        return VarCache.hasReceivedDiffs();
    }


    //remove all vars data . used when switching profiles
    public static void clearUserContent() {
        VarCache.clearUserContent();
    }



    public static void forceContentUpdate(@NonNull Runnable callback) {
        // api to  fetch variables forcefully from the server. it is same endpoint as start,
        // but with differen parameters.
        // todo : replace with ct endpopoint

    }


    public static void addVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {


        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.add(handler);
        }
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
        if (areVariablesReceived()) {
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
