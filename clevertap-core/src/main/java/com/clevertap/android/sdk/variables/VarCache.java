package com.clevertap.android.sdk.variables;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.images.FileResourceProvider;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

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

    private final FileResourcesRepoImpl fileResourcesRepoImpl;
    private final CleverTapInstanceConfig instanceConfig;

    private final FileResourceProvider fileResourceProvider;

    public VarCache(
            CleverTapInstanceConfig config,
            Context ctx,
            FileResourcesRepoImpl fileResourcesRepoImpl,
            FileResourceProvider fileResourceProvider
    ) {
        this.variablesCtx = ctx;
        this.instanceConfig = config;
        this.fileResourcesRepoImpl = fileResourcesRepoImpl;
        this.fileResourceProvider = fileResourceProvider;
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

        boolean shouldMerge;
        if (CTVariableUtils.FILE.equals(var.kind())) {
            shouldMerge = defaultValue == null && mergedValue != null;
        } else {
            shouldMerge = defaultValue != null && !defaultValue.equals(mergedValue);
        }

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
            defaultKinds
        );

        mergeVariable(var);
    }

    public synchronized Object getMergedValue(String variableName) {
        Var<?> var = vars.get(variableName);
        if (var != null && CTVariableUtils.FILE.equals(var.kind())) {
            return filePathFromDisk(var.stringValue);
        }

        String[] components = CTVariableUtils.getNameComponents(variableName);
        Object mergedValue = getMergedValueFromComponentArray(components);
        if (mergedValue instanceof Map) {
            return CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(mergedValue));
        } else {
            return mergedValue;
        }
    }

    public synchronized <T> T getMergedValueFromComponentArray(Object[] components) {
        return getMergedValueFromComponentArray(
                components,
                merged != null ? merged : valuesFromClient
        );
    }

    public synchronized <T> T getMergedValueFromComponentArray(Object[] components, Object values) {
        Object mergedPtr = values;
        for (Object component : components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false);
        }
        return JsonUtil.uncheckedCast(mergedPtr);
    }

    public synchronized void loadDiffs(Function0<Unit> func) {
        try {
            String variablesFromCache = loadDataFromCache();
            Map<String, Object> variablesAsMap = JsonUtil.fromJson(variablesFromCache);

            // Update variables with new values. Have to copy the dictionary because a
            // dictionary variable may add a new sub-variable, modifying the variable dictionary.
            HashMap<String, Var<?>> clientRegisteredVars = new HashMap<>(vars);

            applyVariableDiffs(variablesAsMap, clientRegisteredVars);
            startFilesDownload(clientRegisteredVars, func);

        } catch (Exception e) {
            log("Could not load variable diffs.\n" ,e);
        }
    }

    public synchronized void loadDiffsAndTriggerHandlers(Function0<Unit> func) {
        loadDiffs(func);
        triggerGlobalCallbacks();
    }

    public synchronized void updateDiffsAndTriggerHandlers(
            Map<String, Object> diffs,
            Function0<Unit> func
    ) {
        // Update variables with new values. Have to copy the dictionary because a
        // dictionary variable may add a new sub-variable, modifying the variable dictionary.
        HashMap<String, Var<?>> clientRegisteredVars = new HashMap<>(vars);

        applyVariableDiffs(diffs, clientRegisteredVars);
        startFilesDownload(clientRegisteredVars, func);
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

    /** @noinspection unchecked*/
    private void applyVariableDiffs(
            Map<String, Object> diffs,
            HashMap<String, Var<?>> clientRegisteredVars
    ) {
        log("applyVariableDiffs() called with: diffs = [" + diffs + "]");
        if (diffs != null) {
            this.diffs = diffs;
            merged = CTVariableUtils.mergeHelper(valuesFromClient, this.diffs);
            log("applyVariableDiffs: updated value of merged=["+merged+"]" );

            for (Map.Entry<String, Var<?>> entry : clientRegisteredVars.entrySet()) {
                String name = entry.getKey();
                Var<?> var = vars.get(name);
                if (var != null) {
                    var.update();
                }
            }
        }
    }

    private void startFilesDownload(
            HashMap<String, Var<?>> clientRegisteredVars,
            Function0<Unit> func
    ) {

        if (clientRegisteredVars.isEmpty()) {
            log("There are no variables registered by the client. Not downloading files " +
                    "& posting global callbacks");
            return;
        }

        StringBuilder skipped = new StringBuilder();
        skipped.append("Skipped these file vars cause urls are not present :");
        skipped.append("\n");

        StringBuilder added = new StringBuilder();
        added.append("Adding these files to download :");
        added.append("\n");

        ArrayList<Pair<String, CtCacheType>> urls = new ArrayList<>();

        for (Map.Entry<String, Var<?>> entry : clientRegisteredVars.entrySet()) {
            String name = entry.getKey();
            Var<?> var = vars.get(name);

            if (var != null && var.kind().equals(CTVariableUtils.FILE)) {

                String url = var.rawFileValue();

                if (url != null) {
                    boolean isFileCached = fileResourceProvider.isFileCached(url);
                    if (!isFileCached) {
                        urls.add(new Pair<>(url, CtCacheType.FILES));
                        added.append(name).append(" : ").append(url);
                        added.append("\n");
                    }

                } else {
                    skipped.append(name);
                    skipped.append("\n");
                }
            }
        }
        log(skipped.toString());
        log(added.toString());

        if (urls.isEmpty()) {
            func.invoke(); // triggers global files callbacks
            return;
        }
        fileResourcesRepoImpl.preloadFilesAndCache(
                urls,
                downloadAllBlock -> {
                    // triggers global files callbacks to client
                    func.invoke();
                    return null;
                }
        );
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
        HashMap<String, Var<?>> clientRegisteredVars = new HashMap<>(vars);
        for (String name : clientRegisteredVars.keySet()) {
            Var<?> var = vars.get(name);
            if (var != null) {
                var.clearStartFlag();
            }
        }

        // 2. reset server values for previous user
        applyVariableDiffs(new HashMap<>(), clientRegisteredVars);

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

    public String filePathFromDisk(String url) {
        return fileResourceProvider.cachedFilePath(url);
    }

    public void fileVarUpdated(Var<String> fileVar) {
        String url = fileVar.rawFileValue();
        if (fileResourceProvider.isFileCached(url)) {
            // if present in cache
            fileVar.triggerFileIsReady();
        } else {
            List<Pair<String, CtCacheType>> list = new ArrayList<>();
            list.add(new Pair<>(url, CtCacheType.FILES));
            fileResourcesRepoImpl.preloadFilesAndCache(
                    list,
                    downloadAllBlock -> {
                        fileVar.triggerFileIsReady();
                        return null;
                    }
            );
        }
    }
}
