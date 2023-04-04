package com.clevertap.android.sdk.variables;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.BuildConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.variables.callbacks.CacheUpdateCallback;
import com.clevertap.android.sdk.variables.callbacks.VariableCallback;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback;

import java.util.List;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;


/**
 * Package Private class to not allow external access to working of Variables
 *
 * @author Ansh Sachdeva
 */
public class CTVariables {

    private boolean variableResponseReceived = false;
    private final List<VariablesChangedCallback> variablesChangedCallbacks = new ArrayList<>();
    private final List<VariablesChangedCallback> oneTimeVariablesChangedCallbacks = new ArrayList<>();
    private final CacheUpdateCallback triggerVariablesChanged = () -> {
        synchronized (variablesChangedCallbacks) {
            for (VariablesChangedCallback callback : variablesChangedCallbacks) {
                Utils.runOnUiThread(callback);
            }
        }
        synchronized (oneTimeVariablesChangedCallbacks) {
            for (VariablesChangedCallback callback : oneTimeVariablesChangedCallbacks) {
                Utils.runOnUiThread(callback);
            }
            oneTimeVariablesChangedCallbacks.clear();
        }
    };

    private final VarCache varCache;

    private static void log(String msg){
        Logger.v("variables", msg);
    }

    public CTVariables(final VarCache varCache) {
        this.varCache = varCache;
        this.varCache.setCacheUpdateCallback(triggerVariablesChanged);
    }

    /** WORKING: <br>
     *  0. user calls this function which triggers calls the following functions synchronously :<br><br>
     *      *** {@link VarCache#setCacheUpdateCallback(CacheUpdateCallback)} : this sets a callback in
     *          {@link VarCache} class, which will be triggered once the values are loaded/updated
     *          from the server/cache <br><br>
     *      *** {@link VarCache#loadDiffs()} : this loads the last cached values of Variables from
     *          Shared Preferences, and updates {@link VarCache()#diffs} & {@link VarCache()#merged}
     *          accordingly <br><br>
     *
     *  Note that user's callbacks are *not* triggered during init call
     */
    public void init() {
        log("init() called");
        varCache.loadDiffs();
    }

    /**
     * originally  <a href="https://github.com/Leanplum/Leanplum-Android-SDK/blob/master/AndroidSDKCore/src/main/java/com/leanplum/Leanplum.java#L843">handleStartResponse()</a><br><br>
     * This function is called once the variable data is available in the response of {@link
     * Constants#APP_LAUNCHED_EVENT}/ {@link Constants#WZRK_FETCH} request  <br><br>
     * -- if the json data is correct we convert json to map and call {@link VarCache#updateDiffsAndTriggerHandlers(Map)}.<br><br>
     * -- else we call {@link VarCache#loadDiffsAndTriggerHandlers()} to set data from cache again
     *
     * @param response JSONObject . must pass the the json directly (i.e {key:value} and not {vars:{key:value}})
     */
    public void handleVariableResponse(@Nullable final JSONObject response, @Nullable FetchVariablesCallback callback) {
        log("handleVariableResponse() called with: response = [" + response + "]");
        setVariableResponseReceived(true);

        boolean jsonHasVariableData = response != null;
        try {
            if (!jsonHasVariableData) {
                varCache.loadDiffsAndTriggerHandlers();
                if(callback!=null)callback.onVariablesFetched(false);
            } else {
                Map<String, Object> variableDiffs = JsonUtil.mapFromJson(response);
                variableDiffs = CTVariableUtils.convertFlatMapToNestedMaps(variableDiffs);
                varCache.updateDiffsAndTriggerHandlers(variableDiffs);
                if(callback!=null)callback.onVariablesFetched(true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    /**
     * clear current variable data.can be used during profile switch
     */
    public void clearUserContent() {
        varCache.reset();
    }


    public static boolean isDevelopmentMode() {
        return BuildConfig.DEBUG;
    }

    /**
     * Note: it is necessary to call the listeners immediately after user adds a listener using this function
     * because there is no guarantee that the newly added listener is going to be called later. <br><br>
     * we only receive variable data (and trigger handlers) on certain event calls that happens asynchronously,
     * so if client registers the listeners after such events have triggered,
     * they should still be able to get those previously updated values
     */
    public void addVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        synchronized (variablesChangedCallbacks) {
            variablesChangedCallbacks.add(callback);
        }

        if (varCache.hasReceivedDiffs()) {
            callback.variablesChanged();
        }
    }

    /**
     * Note: Following the similar logic as mentioned in the comment on {@link #addVariablesChangedCallback},
     * if a user adds a OneTimeVariablesChangedHandler using this function AFTER the variable data is received,
     * (and one time handlers already triggered), the user will not have their newly
     * added handlers triggered ever. therefore, it is necessary to trigger the user's handlers
     * immediately once this function is called
     */
    public void addOneTimeVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        if (varCache.hasReceivedDiffs()) {
            callback.variablesChanged();
        } else {
            synchronized (oneTimeVariablesChangedCallbacks) {
                oneTimeVariablesChangedCallbacks.add(callback);
            }
        }
    }

    public void removeVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        synchronized (variablesChangedCallbacks) {
            variablesChangedCallbacks.remove(callback);
        }
    }

    public void removeOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback callback) {
        synchronized (oneTimeVariablesChangedCallbacks) {
            oneTimeVariablesChangedCallbacks.remove(callback);
        }
    }

    public void removeAllVariablesChangedCallbacks() {
        synchronized (variablesChangedCallbacks) {
            variablesChangedCallbacks.clear();
        }
    }

    public void removeAllOneTimeVariablesChangedCallbacks() {
        synchronized (oneTimeVariablesChangedCallbacks) {
            oneTimeVariablesChangedCallbacks.clear();
        }
    }

    /**
     * originally <a href="https://github.com/Leanplum/Leanplum-Android-SDK/blob/master/AndroidSDKCore/src/main/java/com/leanplum/Leanplum.java#L1195">Leanplum.hasStarted()</a>
     * <br>
     * This flag indicates whether or not the SDK is still in process of receiving a response
     * from the server. <br>
     * This is used to print warnings in logs (see {@link Var#warnIfNotStarted()},
     * and prevent listeners from triggering ( see {@link Var#update()} and {@link
     * Var#addValueChangedCallback(VariableCallback)}
     * <br>
     * <br>
     *
     * @return value of {@link #variableResponseReceived  }
     */
    public Boolean isVariableResponseReceived() {
        return variableResponseReceived;
    }

    /**
     * originally <a href="https://github.com/Leanplum/Leanplum-Android-SDK/blob/master/AndroidSDKCore/src/main/java/com/leanplum/internal/LeanplumInternal.java#L705">LeanplumInternal.setHasStarted(started)</a>
     * <br><br>
     * This is set to : <br>
     * - true, when SDK receives a response for Variable data (in {@link #handleVariableResponse}) (The function is
     * always triggerred, even when api fails) <br> <br>
     *
     * <br>
     * <br>
     *
     * @param responseReceived : a boolean to be set {@link #variableResponseReceived  }
     */
    public void setVariableResponseReceived(boolean responseReceived) {
        variableResponseReceived = responseReceived; // might not be needed
    }

    VarCache getVarCache(){
        return varCache;
    }


}
