package com.clevertap.android.sdk.variables;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
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
    private static void log(String msg){
        Logger.d("variables", msg);
    }

    private static void log(String msg,Throwable t){
        Logger.d("variables", msg, t);
    }

    private final Map<String, Object> valuesFromClient = new HashMap<>();

    private final Map<String, Var<?>> vars = new ConcurrentHashMap<>();

    private final Map<String, String> defaultKinds = new HashMap<>();

    private Runnable globalCallbacksRunnable = null;

    private Map<String, Object> diffs = new HashMap<>();

    public Object merged = null;

    // README: Do not forget reset the value of new fields in the reset() method.

    private final Context variablesCtx;
    private final CleverTapInstanceConfig instanceConfig;

    public VarCache(CleverTapInstanceConfig config, Context ctx) {
        this.variablesCtx = ctx;
        this.instanceConfig = config;
    }

    private void storeDataInCache(@NonNull String data){
        log("storeDataInCache() called with: data = [" + data + "]");
        String cacheKey = StorageHelper.storageKeyWithSuffix(instanceConfig, Constants.CACHED_VARIABLES_KEY);
        try {
            StorageHelper.putString(variablesCtx, cacheKey, data);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private String loadDataFromCache(){
        String cacheKey = StorageHelper.storageKeyWithSuffix(instanceConfig, Constants.CACHED_VARIABLES_KEY);
        String cache =  StorageHelper.getString(variablesCtx,cacheKey, "{}");
        log("VarCache loaded cache data:\n" + cache);
        return  cache;
    }

    /**
     * If invoked with a.b.c.d, updates a, a.b, a.b.c, but a.b.c.d is left for the Var.define.
     *
     * @param var
     */
    @VisibleForTesting
    void mergeVariable(@NonNull Var<?> var) {
        if (merged == null) {
            log("mergeVariable() called, but `merged` member is null.");
            return;
        } else if (!(merged instanceof Map<?, ?>)) {
            log("mergeVariable() called, but `merged` member is not of Map type.");
            return;
        }

        String firstComponent = var.nameComponents()[0];
        Object defaultValue = valuesFromClient.get(firstComponent);
        Map<String, Object> mergedMap = JsonUtil.uncheckedCast(merged);
        Object mergedValue = mergedMap.get(firstComponent);

        boolean shouldMerge =
            (defaultValue == null && mergedValue != null) ||
                (defaultValue != null && !defaultValue.equals(mergedValue));

        if (shouldMerge) {
            Object newValue = CTVariableUtils.mergeHelper(defaultValue, mergedValue);

            mergedMap.put(firstComponent, newValue);

            StringBuilder name = new StringBuilder(firstComponent);
            for (int i = 1; i < var.nameComponents().length; i++) {
                Var<?> existing = vars.get(name.toString());
                if (existing != null) {
                    existing.update();
                }
                name.append('.').append(var.nameComponents()[i]);
            }
        }
    }

    public synchronized void registerVariable(@NonNull Var<?> var) {
        log( "registerVariable() called with: var = [" + var + "]");
        vars.put(var.name(), var);

        Object defaultValue = var.defaultValue();
        if (defaultValue instanceof Map) {
            defaultValue = CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(defaultValue));
        }
        CTVariableUtils.updateValuesAndKinds(
            var.name(),
            var.nameComponents(),
            defaultValue,
            var.kind(),
            valuesFromClient,
            defaultKinds);

        mergeVariable(var);

    }

    public synchronized Object getMergedValue(String variableName) {
        String[] components = CTVariableUtils.getNameComponents(variableName);
        Object mergedValue = getMergedValueFromComponentArray(components);
        if (mergedValue instanceof Map) {
            return CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(mergedValue));
        } else {
            return mergedValue;
        }
    }

    public synchronized <T> T getMergedValueFromComponentArray(Object[] components) {
        return getMergedValueFromComponentArray(components, merged != null ? merged : valuesFromClient);
    }

    public synchronized <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false);
        }
        return JsonUtil.uncheckedCast(mergedPtr);
    }

    public synchronized void loadDiffs() {
        try {
            String variablesFromCache = loadDataFromCache();
            Map<String, Object> variablesAsMap = JsonUtil.fromJson(variablesFromCache);
            applyVariableDiffs(variablesAsMap);

        } catch (Exception e) {
            log("Could not load variable diffs.\n" ,e);
        }
    }

    public synchronized void loadDiffsAndTriggerHandlers() {
        loadDiffs();
        triggerGlobalCallbacks();
    }

    public synchronized void updateDiffsAndTriggerHandlers(Map<String, Object> diffs) {
        applyVariableDiffs(diffs);
        saveDiffsAsync();
        triggerGlobalCallbacks();
    }

    private void saveDiffsAsync() {
        Task<Void> task = CTExecutorFactory.executors(instanceConfig).postAsyncSafelyTask();
        task.execute("VarCache#saveDiffsAsync", () -> {
            saveDiffs();
            return null;
        });
    }

    @WorkerThread
    private void saveDiffs() {
        log("saveDiffs() called");
        String variablesCipher = JsonUtil.toJson(diffs);
        storeDataInCache(variablesCipher);
    }

    private void applyVariableDiffs(Map<String, Object> diffs) {
        log("applyVariableDiffs() called with: diffs = [" + diffs + "]");
        if (diffs != null) {
            this.diffs = diffs;
            merged = CTVariableUtils.mergeHelper(valuesFromClient, this.diffs);
            log("applyVariableDiffs: updated value of merged=["+merged+"]" );

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

    private synchronized void triggerGlobalCallbacks() {
        if (globalCallbacksRunnable != null) {
            globalCallbacksRunnable.run();
        }
    }

    public JSONObject getDefineVarsData(){
        return CTVariableUtils.getFlatVarsJson(valuesFromClient,defaultKinds);
    }

    public synchronized void clearUserContent() {
        log("Clear user content in VarCache");
        // 1. clear Var state to allow callback invocation when server values are downloaded
        for (String name : new HashMap<>(vars).keySet()) {
            Var<?> var = vars.get(name);
            if (var != null) {
                var.clearStartFlag();
            }
        }

        // 2. reset server values for previous user
        applyVariableDiffs(new HashMap<>());

        // 3. reset data in shared prefs
        saveDiffsAsync();
    }

    public synchronized <T> Var<T> getVariable(String name) {
        return JsonUtil.uncheckedCast(vars.get(name));
    }

    @VisibleForTesting
    int getVariablesCount() {
        return vars.size();
    }

    public synchronized void setGlobalCallbacksRunnable(Runnable runnable) {
        globalCallbacksRunnable = runnable;
    }

}
