package com.clevertap.android.sdk.variables

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import androidx.annotation.RestrictTo

/**
 * Variable cache.
 *
 * @author Ansh Sachdeva.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class VarCache(
    private val instanceConfig: CleverTapInstanceConfig,
    private val variablesCtx: Context,
    private val fileResourcesRepoImpl: FileResourcesRepoImpl,
    private val variablesRepo: VariablesRepo
) {
    private val valuesFromClient: MutableMap<String, Any?> = HashMap()
    private val vars: MutableMap<String, Var<*>> = ConcurrentHashMap()
    private val defaultKinds: MutableMap<String, String?> = HashMap()

    var merged: Any? = null
    private var globalCallbacksRunnable: Runnable? = null

    // README: Do not forget reset the value of new fields in the reset() method.
    private var diffs: Map<String, Any?> = HashMap()

    private var abVariants: List<Map<String, Any?>> = ArrayList()

    /**
     * If invoked with a.b.c.d, updates a, a.b, a.b.c, but a.b.c.d is left for the Var.define.
     *
     * @param variable a registered variable (its name components are set by [Var.define])
     */
    @VisibleForTesting
    internal fun mergeVariable(variable: Var<*>) {
        val merged = merged
        if (merged == null) {
            log("mergeVariable() called, but `merged` member is null.")
            return
        } else if (merged !is Map<*, *>) {
            log("mergeVariable() called, but `merged` member is not of Map type.")
            return
        }

        val nameComponents = checkNotNull(variable.nameComponents())
        val firstComponent = nameComponents[0]
        val defaultValue = valuesFromClient[firstComponent]
        val mergedMap: MutableMap<String, Any?> = JsonUtil.uncheckedCast(merged)
        val mergedValue = mergedMap[firstComponent]

        val shouldMerge = if (CTVariableUtils.FILE == variable.kind()) {
            defaultValue == null && mergedValue != null
        } else {
            defaultValue != null && defaultValue != mergedValue
        }
        if (!shouldMerge) {
            return
        }

        mergedMap[firstComponent] = CTVariableUtils.mergeHelper(defaultValue, mergedValue)

        val name = StringBuilder(firstComponent)
        for (i in 1 until nameComponents.size) {
            vars[name.toString()]?.update()
            name.append('.').append(nameComponents[i])
        }
    }

    @Synchronized
    open fun registerVariable(variable: Var<*>) {
        log("registerVariable() called with: var = [${variable.name()}]")
        vars[checkNotNull(variable.name())] = variable

        val defaultValue = variable.defaultValue().let {
            if (it is Map<*, *>) {
                CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(it))
            } else {
                it
            }
        }
        CTVariableUtils.updateValuesAndKinds(
            variable.name(),
            variable.nameComponents(),
            defaultValue,
            variable.kind(),
            valuesFromClient,
            defaultKinds
        )

        mergeVariable(variable)
    }

    @Synchronized
    open fun getMergedValue(variableName: String?): Any? {
        val variable = vars[variableName]
        if (variable != null && CTVariableUtils.FILE == variable.kind()) {
            return filePathFromDisk(variable.stringValue)
        }

        val components = CTVariableUtils.getNameComponents(variableName)
        val mergedValue: Any? = getMergedValueFromComponentArray(components)
        return if (mergedValue is Map<*, *>) {
            CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(mergedValue))
        } else {
            mergedValue
        }
    }

    @Synchronized
    open fun <T> getMergedValueFromComponentArray(components: Array<out Any?>): T =
        getMergedValueFromComponentArray(components, merged ?: valuesFromClient)

    @Synchronized
    open fun <T> getMergedValueFromComponentArray(components: Array<out Any?>, values: Any?): T {
        var mergedPtr = values
        for (component in components) {
            mergedPtr = CTVariableUtils.traverse(mergedPtr, component, false)
        }
        return JsonUtil.uncheckedCast(mergedPtr)
    }

    @Synchronized
    open fun loadDiffs(func: Function0<Unit>) {
        handleVariablesData(func)
        handleAbVariantsData()
    }

    private fun handleVariablesData(func: Function0<Unit>) {
        try {
            val variablesFromCache = variablesRepo.loadDataFromCache()
            val variablesAsMap = JsonUtil.fromJson(variablesFromCache)

            // Update variables with new values. Have to copy the dictionary because a
            // dictionary variable may add a new sub-variable, modifying the variable dictionary.
            val clientRegisteredVars = HashMap(vars)

            applyVariableDiffs(variablesAsMap, clientRegisteredVars)
            startFilesDownload(clientRegisteredVars, func)
        } catch (e: Exception) {
            log("Could not load variable diffs.\n", e)
        }
    }

    private fun handleAbVariantsData() {
        try {
            val variantsFromCache = variablesRepo.loadVariantsFromCache()
            val variants = if (variantsFromCache == null) { JSONArray() } else { JSONArray(variantsFromCache) }
            abVariants = JsonUtil.listFromJsonSafe(variants)
        } catch (e: Exception) {
            log("Could not load variants", e)
        }
    }

    @Synchronized
    open fun loadDiffsAndTriggerHandlers(func: Function0<Unit>) {
        loadDiffs(func)
        triggerGlobalCallbacks()
    }

    @Synchronized
    open fun updateAbVariants(variants: List<Map<String, Any?>>) {
        abVariants = variants
    }

    @Synchronized
    open fun updateDiffsAndTriggerHandlers(
        diffs: Map<String, Any?>?,
        func: Function0<Unit>
    ) {
        // Update variables with new values. Have to copy the dictionary because a
        // dictionary variable may add a new sub-variable, modifying the variable dictionary.
        val clientRegisteredVars = HashMap(vars)

        applyVariableDiffs(diffs, clientRegisteredVars)
        startFilesDownload(clientRegisteredVars, func)
        saveDiffsAsync()
        triggerGlobalCallbacks()
    }

    private fun saveDiffsAsync() {
        val task = CTExecutorFactory.executors(instanceConfig).postAsyncSafelyTask<Void?>()
        task.execute("VarCache#saveDiffsAsync") {
            saveDiffs()
            saveAbVariants()
            null
        }
    }

    @WorkerThread
    private fun saveDiffs() {
        log("saveDiffs() called")
        JsonUtil.toJson(diffs)?.let { variablesRepo.storeDataInCache(it) }
    }

    @WorkerThread
    private fun saveAbVariants() {
        log("saveAbVariables() called")
        JsonUtil.toJson(abVariants)?.let { variablesRepo.storeVariantsInCache(it) }
    }

    private fun applyVariableDiffs(
        diffs: Map<String, Any?>?,
        clientRegisteredVars: Map<String, Var<*>>
    ) {
        log("applyVariableDiffs() called with: diffs = [$diffs]")
        if (diffs == null) {
            return
        }
        this.diffs = diffs
        merged = CTVariableUtils.mergeHelper(valuesFromClient, diffs)
        log("applyVariableDiffs: updated value of merged=[$merged]")

        for (name in clientRegisteredVars.keys) {
            vars[name]?.update()
        }
    }

    private fun startFilesDownload(clientRegisteredVars: Map<String, Var<*>>, func: () -> Unit) {
        if (clientRegisteredVars.isEmpty()) {
            log("There are no variables registered by the client. Not downloading files & posting global callbacks")
            return
        }

        val skipped = StringBuilder("Skipped these file vars cause urls are not present :\n")
        val added = StringBuilder("Adding these files to download :\n")
        val urls = ArrayList<Pair<String, CtCacheType>>()

        for (name in clientRegisteredVars.keys) {
            collectFileVarUrl(name = name, skipped = skipped, added = added, urls = urls)
        }
        log(skipped.toString())
        log(added.toString())

        if (urls.isEmpty()) {
            func() // triggers global files callbacks
            return
        }
        // triggers global files callbacks to client
        fileResourcesRepoImpl.preloadFilesAndCache(urls, completionCallback = { func() })
    }

    /**
     * If [name] is a file variable whose file is not cached yet, adds its url to [urls]. File
     * variables without a url are listed in [skipped], queued ones in [added].
     */
    private fun collectFileVarUrl(
        name: String,
        skipped: StringBuilder,
        added: StringBuilder,
        urls: MutableList<Pair<String, CtCacheType>>
    ) {
        val variable = vars[name]
        // a registered variable always has a kind (set by Var.define)
        if (variable == null || checkNotNull(variable.kind()) != CTVariableUtils.FILE) {
            return
        }
        val url = variable.rawFileValue()
        if (url == null) {
            skipped.append(name).append("\n")
            return
        }
        val isFileCached = FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger).isFileCached(url)
        if (!isFileCached) {
            urls.add(Pair(url, CtCacheType.FILES))
            added.append(name).append(" : ").append(url).append("\n")
        }
    }

    @Synchronized
    private fun triggerGlobalCallbacks() {
        globalCallbacksRunnable?.run()
    }

    open fun getDefineVarsData(): JSONObject = CTVariableUtils.getFlatVarsJson(valuesFromClient, defaultKinds)

    @Synchronized
    open fun clearUserContent() {
        log("Clear user content in VarCache")
        // 1. clear Var state to allow callback invocation when server values are downloaded
        val clientRegisteredVars = HashMap(vars)
        for (name in clientRegisteredVars.keys) {
            vars[name]?.clearStartFlag()
        }

        // 2. reset server values for previous user
        applyVariableDiffs(HashMap(), clientRegisteredVars)
        abVariants = ArrayList()

        // 3. reset data in shared prefs
        saveDiffsAsync()
    }

    @Synchronized
    open fun <T> getVariable(name: String?): Var<T>? = JsonUtil.uncheckedCast(vars[name])

    @get:VisibleForTesting
    internal val variablesCount: Int
        get() = vars.size

    @Synchronized
    open fun setGlobalCallbacksRunnable(runnable: Runnable?) {
        globalCallbacksRunnable = runnable
    }

    open fun filePathFromDisk(url: String?): String? =
        FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger).cachedFilePath(url)

    open fun fileVarUpdated(fileVar: Var<String>) {
        val url = fileVar.rawFileValue()
        val task = CTExecutorFactory.executors(instanceConfig).ioTask<Boolean>()
        task.addOnSuccessListener { isCached ->
            if (isCached || url == null) {
                // a null url counts as cached (see the task below)
                fileVar.triggerFileIsReady()
            } else {
                fileResourcesRepoImpl.preloadFilesAndCache(
                    listOf(Pair(url, CtCacheType.FILES)),
                    completionCallback = { fileVar.triggerFileIsReady() }
                )
            }
        }
        task.execute("isFileCached") {
            url == null || FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger).isFileCached(url)
        }
    }

    open fun variants(): List<Map<String, Any?>> = abVariants

    private companion object {
        private fun log(msg: String) = Logger.d("variables", msg)

        private fun log(msg: String, t: Throwable) = Logger.d("variables", msg, t)
    }
}
