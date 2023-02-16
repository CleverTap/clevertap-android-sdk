package com.clevertap.android.sdk.feat_variable;

import android.content.Context;
import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.BuildConfig;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.feat_variable.callbacks.VariablesChangedCallback;
import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;


public class CTVariables {
    private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers = new ArrayList<>();
    private static final ArrayList<VariablesChangedCallback> oneTimeVariablesChangedHandlers = new ArrayList<>();
    private static Context context;
    private static boolean variableResponseReceived = false;
    public static final boolean hasSdkError =false; //todo // keep it? @darshan
    //public static final String VARS_FROM_CODE = "varsFromCode";
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Boolean variableResponseReceived(){ //originally hasStarted
        //its true if server response for "start" api is received.
        return variableResponseReceived;//TODO:@Ansh is variableResponseReceived and hasStarted same?
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void setVariableResponseReceived(boolean responseReceived){ //originally setHasStarted()
        variableResponseReceived = responseReceived; // might not be needed
    }

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
        VarCache.loadDiffs();
        VarCache.setCacheUpdateBlock(CTVariables::triggerVariablesChanged);
        requestVariableDataFromServer();
    }

    @Discouraged(message = "remove after testing")
    public static synchronized void initWithApiFailure(){
        checkFailSafe();
        VarCache.loadDiffs();
        VarCache.setCacheUpdateBlock(CTVariables::triggerVariablesChanged);
        //requestVariableDataFromServer();

        Logger.v("requesting data from server");

        handleStartResponse(null);
    }



    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void requestVariableDataFromServer() {
        Logger.v("requesting data from server");
        //CTVariables.setHasStarted(false); //todo @hristo : whenever requesting data from server on wzrk_fetch, we should set has started as false, right? because at that time too the variables should not be accessed / listeners to be called
        //VarCache.setHasReceivedDiffs(false);;

        FakeServer.requestBE(jsonObject -> { //todo replace with actual api
            handleStartResponse(jsonObject);
            return kotlin.Unit.INSTANCE;
        });

    }

    private static void checkFailSafe() {
        // this is a situation where some error happened in ct sdk. so we just apply empty to all var cache
        if (hasSdkError) {
            setVariableResponseReceived(true);
            triggerVariablesChanged();
            VarCache.updateDiffs(new HashMap<>());
        }


    }

    private static void handleStartResponse(@Nullable final JSONObject response) {
        setVariableResponseReceived(true);
        boolean jsonHasVariableData = response!=null && true; //check if response was successful, like response.data!=null //todo add logic as per backend response structure
        try {
            if (!jsonHasVariableData) {
                // Load the variables that were stored on the device from the last session.
                // this will also invoke user's callback, but with values from last session/shared prefs
                VarCache.loadDiffsAndTriggerHandlers();
            } else {
                Map<String, Object> variableDiffs = CTVariableUtils.mapFromJson(response.optJSONObject(VARS));
                VarCache.updateDiffs(variableDiffs);
                //if (isDevelopmentModeEnabled) {
                //    Map<String, Object> locals = CTVariableUtils.mapFromJson(response.optJSONObject(VARS_FROM_CODE));
                //    VarCache.setDevModeValuesFromServer(locals);
                //}
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



    @Discouraged(message = "Be Very careful when calling this api. This should only be called in debug mode")
    public static void pushVariablesToServer(Runnable onComplete) {
        VarCache.pushVariablesToServer(onComplete);
    }
    public static boolean isInDevelopmentMode(){
        return BuildConfig.DEBUG;
    }


    public static void addVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.add(handler);
        }
        // it is needed to call the listeners immediately after being added because there is
        // no guarentee that it is going to be called later. we only receive diffs and call the
        // listeners in CTVariables.init() response, so if app registers the listeners later,
        // it should also get those previously updated values
        if (VarCache.hasReceivedDiffs()) {
            handler.variablesChanged();
        }
    }

    public static void addOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        // we are directly calling the listener to handle the scenario where some of the handlers
        // were already registered previously and new handlers are added later. as per the logic of
        // triggerVariablesChanged(), the listeners will only be called once, and therefore new
        // listeners won't get called. so we trigger them immediately
        if (VarCache.hasReceivedDiffs()) {
            handler.variablesChanged();
        } else {
            synchronized (oneTimeVariablesChangedHandlers) {
                oneTimeVariablesChangedHandlers.add(handler);
            }
        }
    }

    public static void removeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.remove(handler);
        }
    }
    public static void removeOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) { //removeOnceVariablesChangedAndNoDownloadsPendingHandler

        synchronized (oneTimeVariablesChangedHandlers) {
            oneTimeVariablesChangedHandlers.remove(handler);
        }
    }

    public static void removeAllVariablesChangedHandler() {
        synchronized (variablesChangedHandlers) {
            variablesChangedHandlers.clear();
        }
    }
    public static void removeAllOneTimeVariablesChangedHandler() { //removeOnceVariablesChangedAndNoDownloadsPendingHandler

        synchronized (oneTimeVariablesChangedHandlers) {
            oneTimeVariablesChangedHandlers.clear();
        }
    }
}
