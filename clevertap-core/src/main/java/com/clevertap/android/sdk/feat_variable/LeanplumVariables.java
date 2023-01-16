package com.clevertap.android.sdk.feat_variable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class LeanplumVariables {
    private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers = new ArrayList<>(); //needed
    private static final ArrayList<VariablesChangedCallback> onceNoDownloadsHandlers = new ArrayList<>();// needed : its actually onceVariableChangedAndNoDownloadsPending (basically same as first in case of variables but runs only once),

    private static Context context;
    public static final  boolean isDevelopmentModeEnabled = false;


    private static final boolean hasSdkError =false;
    public static final String VARS_FROM_CODE = "varsFromCode";
    public static final String VARS = "vars";


    //needed by varcache to create shared prefs. so need a way to pass context there
    @Nullable
    public static Context getContext() {
        if (context == null) {
            Logger.v("Your application context is not set. You should call Leanplum.setApplicationContext(this) or " + "LeanplumActivityHelper.enableLifecycleCallbacks(this) in your application's " + "onCreate method, or have your application extend LeanplumApplication.");
        }
        return context;
    }
    public static void setContext(Context context) {
        LeanplumVariables.context = context;
    }

    static synchronized void init(){
        // this is a situation where some error happened in ct sdk. so we just apply empty to all var cache
        if (hasSdkError) {
            //LeanplumInternal.setHasStarted(true);
            //LeanplumInternal.setStartSuccessful(true);
            triggerVariablesChanged();
            VarCache.applyVariableDiffs(new HashMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), "", "");
        }
       else {
            // we first load variables from cache . and set silent so as to not update listeners. then we reset silent to false, so next time when we load from the server, we aree able to call listeners
            VarCache.setSilent(true);
            VarCache.loadDiffs();
            VarCache.setSilent(false);

            // we register an internal listener to update client's listenere whenever the load diffs updates the variables
            VarCache.onUpdate(LeanplumVariables::triggerVariablesChanged);

            //todo code to download clevertap variable data and pass it in
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
        boolean jsonHasVariableData = true; //check if response was successful, like response.data!=null
        try {
            if (!jsonHasVariableData) {
                //LeanplumInternal.setHasStarted(true);
                // Load the variables that were stored on the device from the last session.this will also invoke user's callback, but with values from last session/shared prefs
                VarCache.loadDiffs();
            } else {
                Map<String, Object> values = new HashMap<>();  //JsonConverter.mapFromJson(response.optJSONObject(VARS));  //get vars from json //todo
                VarCache.applyVariableDiffs(values);
                if (isDevelopmentModeEnabled) {
                    HashMap<String, Object> valuesFromCode = new HashMap<>(); // response.optJSONObject(VARS_FROM_CODE); //todo
                    VarCache.setDevModeValuesFromServer(valuesFromCode);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void addVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        if (handler == null) {
            Logger.v("addVariablesChangedHandler - Invalid handler parameter provided.");
            return;
        }

        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.add(handler);
        }
        if (VarCache.hasReceivedDiffs()) {
            handler.variablesChanged();
        }
    }


    public static void removeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        if (handler == null) {
            Logger.v("removeVariablesChangedHandler - Invalid handler parameter provided.");
            return;
        }

        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.remove(handler);
        }
    }


    private static void triggerVariablesChanged() {
        synchronized (variablesChangedHandlers) {
            for (VariablesChangedCallback callback : variablesChangedHandlers) {
                //OperationQueue.sharedInstance().addUiOperation(callback); // replace with ct implemnetation of ui thread executor
            }
        }
        synchronized (onceNoDownloadsHandlers) {
            for (VariablesChangedCallback callback : onceNoDownloadsHandlers) {
                //OperationQueue.sharedInstance().addUiOperation(callback); // replace with ct implemnetation of ui thread executor
            }
            onceNoDownloadsHandlers.clear();
        }
    }



    // rename to addOnceVariablesChanged
    public static void addOnceVariablesChangedAndNoDownloadsPendingHandler(@NonNull VariablesChangedCallback handler) {
        if (handler == null) {
            Logger.v("addOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter" + " provided.");
            return;
        }

        if (areVariablesReceivedAndNoDownloadsPending()) {
            handler.variablesChanged();
        } else {
            synchronized (onceNoDownloadsHandlers) {
                onceNoDownloadsHandlers.add(handler);
            }
        }
    }

    //todo change to include only variables
    static boolean areVariablesReceivedAndNoDownloadsPending() {
        return VarCache.hasReceivedDiffs();
    }


    //
    public static void removeOnceVariablesChangedAndNoDownloadsPendingHandler(@NonNull VariablesChangedCallback handler) {
        if (handler == null) {
            Logger.v("removeOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter provided.");
            return;
        }

        synchronized (onceNoDownloadsHandlers) {
            onceNoDownloadsHandlers.remove(handler);
        }
    }

    // leanplum's implementation of fetching variables forcefully from the server. it is same endpoint as start, but with differen parameters. todo : replace with ct endpopoint
    public static void forceContentUpdate(@NonNull Runnable callback) {

    }
    


    //todo needed?
    /*Traverses the variable structure with the specified path. Path components can be either strings representing keys in a dictionary, or integers representing indices in a list.*/
    @Nullable
    public static Object objectForKeyPathComponents(@NonNull Object[] pathComponents) {
        try {
            return VarCache.getMergedValueFromComponentArray(pathComponents);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    
    //remove all vars data . used when switching profiles
    public static void clearUserContent() {
        VarCache.clearUserContent();
    }



}
