package com.clevertap.android.sdk.variables;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
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

    private boolean hasVarsRequestCompleted = false;
    private boolean preRegisteredFilesDownloaded = false;

    private final List<VariablesChangedCallback> variablesChangedCallbacks = new ArrayList<>();
    private final List<VariablesChangedCallback> oneTimeVariablesChangedCallbacks = new ArrayList<>();
    private final List<VariablesChangedCallback> variablesChangedCallbacksNoDownloadsPending = new ArrayList<>();
    private final List<VariablesChangedCallback> oneTimeVariablesChangedCallbacksNoDownloadsPending = new ArrayList<>();

    private final VarCache varCache;

    private static void logD(String msg){
        Logger.d("variables", msg);
    }

    public CTVariables(final VarCache varCache) {
        this.varCache = varCache;
        Runnable triggerGlobalCallbacks = () -> {
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
        this.varCache.setGlobalCallbacksRunnable(triggerGlobalCallbacks);
    }

    public void init() {
        logD("init() called");
        varCache.loadDiffs(() -> null);
    }

    public void handleVariableResponse(
        @Nullable JSONObject response,
        @Nullable FetchVariablesCallback fetchCallback
    ) {
        logD("handleVariableResponse() called with: response = [" + response + "]");

        if (response == null) {
            handleVariableResponseError(fetchCallback);
        } else {
            handleVariableResponseSuccess(response, fetchCallback);
        }
    }

    public void handleVariableResponseError(@Nullable FetchVariablesCallback fetchCallback) {
        if (!hasVarsRequestCompleted()) {
            setHasVarsRequestCompleted(true);
            varCache.loadDiffsAndTriggerHandlers(() -> {
                triggerGlobalFilesCallbacks();
                preRegisteredFilesDownloaded = true;
                return null;
            });
        }
        if (fetchCallback != null) {
            fetchCallback.onVariablesFetched(false);
        }
    }

    private void handleVariableResponseSuccess(
        @NonNull JSONObject response,
        @Nullable FetchVariablesCallback fetchCallback
    ) {
        setHasVarsRequestCompleted(true);
        Map<String, Object> variableDiffs = JsonUtil.mapFromJson(response);
        variableDiffs = CTVariableUtils.convertFlatMapToNestedMaps(variableDiffs);
        varCache.updateDiffsAndTriggerHandlers(variableDiffs, () -> {
            triggerGlobalFilesCallbacks();
            preRegisteredFilesDownloaded = true;
            return null;
        });
        if (fetchCallback != null) {
            fetchCallback.onVariablesFetched(true);
        }
    }

    /**
     * Triggers global callbacks for file download updates
     */
    private void triggerGlobalFilesCallbacks() {
        synchronized (variablesChangedCallbacksNoDownloadsPending) {
            for (VariablesChangedCallback callback : variablesChangedCallbacksNoDownloadsPending) {
                Utils.runOnUiThread(callback);
            }
        }
        synchronized (oneTimeVariablesChangedCallbacksNoDownloadsPending) {
            for (VariablesChangedCallback callback : oneTimeVariablesChangedCallbacksNoDownloadsPending) {
                Utils.runOnUiThread(callback);
            }
            oneTimeVariablesChangedCallbacksNoDownloadsPending.clear();
        }
    }

    /**
     * clear current variable data.can be used during profile switch
     */
    public void clearUserContent() {
        logD("Clear user content in CTVariables");
        setHasVarsRequestCompleted(false); // disable callbacks and wait until fetch is finished
        preRegisteredFilesDownloaded = false;
        varCache.clearUserContent();
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

        if (hasVarsRequestCompleted) {
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
        if (hasVarsRequestCompleted) {
            callback.variablesChanged();
        } else {
            synchronized (oneTimeVariablesChangedCallbacks) {
                oneTimeVariablesChangedCallbacks.add(callback);
            }
        }
    }

    /**
     * Note: it is necessary to call the listeners immediately after user adds a listener using this function
     * because there is no guarantee that the newly added listener is going to be called later. <br><br>
     * we only receive variable data (and trigger handlers) on certain event calls that happens asynchronously,
     * so if client registers the listeners after such events have triggered,
     * they should still be able to get those previously updated values
     */
    public void onVariablesChangedAndNoDownloadsPending(@NonNull VariablesChangedCallback callback) {
        synchronized (variablesChangedCallbacksNoDownloadsPending) {
            variablesChangedCallbacksNoDownloadsPending.add(callback);
        }

        if (preRegisteredFilesDownloaded) {
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
    // change name to oncePreSdkInitRegisteredVariablesChangedAndNoDownloadsPending
    public void onceVariablesChangedAndNoDownloadsPending(@NonNull VariablesChangedCallback callback) {
        if (preRegisteredFilesDownloaded) {
            callback.variablesChanged();
        } else {
            synchronized (oneTimeVariablesChangedCallbacksNoDownloadsPending) {
                oneTimeVariablesChangedCallbacksNoDownloadsPending.add(callback);
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

    public Boolean hasVarsRequestCompleted() {
        return hasVarsRequestCompleted;
    }

    public void setHasVarsRequestCompleted(boolean completed) {
        hasVarsRequestCompleted = completed;
    }

    VarCache getVarCache(){
        return varCache;
    }

}
