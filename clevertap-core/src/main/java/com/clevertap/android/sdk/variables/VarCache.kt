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
import com.clevertap.android.sdk.task.OnSuccessListener
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import org.json.JSONArray
import org.json.JSONObject
import java.util.Objects
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * Variable cache.
 *
 * @author Ansh Sachdeva.
 */
internal open class VarCache(
    config: CleverTapInstanceConfig,
    ctx: Context,
    private val fileResourcesRepoImpl: FileResourcesRepoImpl,
    private val variablesRepo: VariablesRepo
) {
    private val valuesFromClient: MutableMap<String, Any?> = HashMap()
    private val vars: MutableMap<String, Var<*>> = ConcurrentHashMap()
    private val defaultKinds: MutableMap<String, String?> = HashMap()
    private val variablesCtx: Context = ctx
    private val instanceConfig: CleverTapInstanceConfig = config

    @JvmField
    var merged: Any? = null
    private var globalCallbacksRunnable: Runnable? = null

    // README: Do not forget reset the value of new fields in the reset() method.
    private var diffs: Map<String, Any?> = HashMap()

    private var abVariants: List<Map<String, Any?>> = ArrayList()

    /**
     * If invoked with a.b.c.d, updates a, a.b, a.b.c, but a.b.c.d is left for the Var.define.
     *
     * @param var
     */
    @VisibleForTesting
    internal fun mergeVariable(`var`: Var<*>) {
        if (merged == null) {
            log("mergeVariable() called, but `merged` member is null.")
            return
        } else if (merged !is Map<*, *>) {
            log("mergeVariable() called, but `merged` member is not of Map type.")
            return
        }

        val firstComponent = `var`.nameComponents()!![0]
        val defaultValue = valuesFromClient[firstComponent]
        val mergedMap: MutableMap<String, Any?> = JsonUtil.uncheckedCast(merged)
        val mergedValue = mergedMap[firstComponent]

        val shouldMerge: Boolean = if (CTVariableUtils.FILE == `var`.kind()) {
            defaultValue == null && mergedValue != null
        } else {
            defaultValue != null && defaultValue != mergedValue
        }

        if (shouldMerge) {
            val newValue = CTVariableUtils.mergeHelper(defaultValue, mergedValue)

            mergedMap[firstComponent] = newValue

            val name = StringBuilder(firstComponent)
            for (i in 1 until `var`.nameComponents()!!.size) {
                val existing = vars[name.toString()]
                existing?.update()
                name.append('.').append(`var`.nameComponents()!![i])
            }
        }
    }

    @Synchronized
    open fun registerVariable(`var`: Var<*>) {
        log("registerVariable() called with: var = [" + `var`.name() + "]")
        vars[`var`.name()!!] = `var`

        var defaultValue: Any? = `var`.defaultValue()
        if (defaultValue is Map<*, *>) {
            defaultValue = CTVariableUtils.deepCopyMap(JsonUtil.uncheckedCast(defaultValue))
        }
        CTVariableUtils.updateValuesAndKinds(
            `var`.name(),
            `var`.nameComponents(),
            defaultValue,
            `var`.kind(),
            valuesFromClient,
            defaultKinds
        )

        mergeVariable(`var`)
    }

    @Synchronized
    open fun getMergedValue(variableName: String?): Any? {
        val `var` = vars[variableName]
        if (`var` != null && CTVariableUtils.FILE == `var`.kind()) {
            return filePathFromDisk(`var`.stringValue)
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
    open fun <T> getMergedValueFromComponentArray(components: Array<out Any?>): T {
        return getMergedValueFromComponentArray(
            components,
            if (merged != null) merged else valuesFromClient
        )
    }

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
            val variants: JSONArray = if (variantsFromCache == null) {
                JSONArray()
            } else {
                JSONArray(variantsFromCache)
            }
            abVariants = Objects.requireNonNull(JsonUtil.listFromJson<Map<String, Any?>>(variants))!!
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
        task.execute("VarCache#saveDiffsAsync", Callable<Void?> {
            saveDiffs()
            saveAbVariants()
            null
        })
    }

    @WorkerThread
    private fun saveDiffs() {
        log("saveDiffs() called")
        val variablesCipher = JsonUtil.toJson(diffs)
        if (variablesCipher != null) {
            variablesRepo.storeDataInCache(variablesCipher)
        }
    }

    @WorkerThread
    private fun saveAbVariants() {
        log("saveAbVariables() called")
        val variantsCipher = JsonUtil.toJson(abVariants)
        if (variantsCipher != null) {
            variablesRepo.storeVariantsInCache(variantsCipher)
        }
    }

    private fun applyVariableDiffs(
        diffs: Map<String, Any?>?,
        clientRegisteredVars: HashMap<String, Var<*>>
    ) {
        log("applyVariableDiffs() called with: diffs = [$diffs]")
        if (diffs != null) {
            this.diffs = diffs
            merged = CTVariableUtils.mergeHelper(valuesFromClient, this.diffs)
            log("applyVariableDiffs: updated value of merged=[$merged]")

            for ((name, _) in clientRegisteredVars) {
                val `var` = vars[name]
                `var`?.update()
            }
        }
    }

    private fun startFilesDownload(
        clientRegisteredVars: HashMap<String, Var<*>>,
        func: Function0<Unit>
    ) {
        if (clientRegisteredVars.isEmpty()) {
            log(
                "There are no variables registered by the client. Not downloading files " +
                        "& posting global callbacks"
            )
            return
        }

        val skipped = StringBuilder()
        skipped.append("Skipped these file vars cause urls are not present :")
        skipped.append("\n")

        val added = StringBuilder()
        added.append("Adding these files to download :")
        added.append("\n")

        val urls = ArrayList<Pair<String, CtCacheType>>()

        for ((name, _) in clientRegisteredVars) {
            val `var` = vars[name]

            if (`var` != null && `var`.kind()!! == CTVariableUtils.FILE) {
                val url = `var`.rawFileValue()

                if (url != null) {
                    val isFileCached = FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger)
                        .isFileCached(url)
                    if (!isFileCached) {
                        urls.add(Pair(url, CtCacheType.FILES))
                        added.append(name).append(" : ").append(url)
                        added.append("\n")
                    }
                } else {
                    skipped.append(name)
                    skipped.append("\n")
                }
            }
        }
        log(skipped.toString())
        log(added.toString())

        if (urls.isEmpty()) {
            func.invoke() // triggers global files callbacks
            return
        }
        val completion: (Map<String, Boolean>) -> Unit = {
            // triggers global files callbacks to client
            func.invoke()
        }
        fileResourcesRepoImpl.preloadFilesAndCache(urls, completion)
    }

    @Synchronized
    private fun triggerGlobalCallbacks() {
        globalCallbacksRunnable?.run()
    }

    open fun getDefineVarsData(): JSONObject {
        return CTVariableUtils.getFlatVarsJson(valuesFromClient, defaultKinds)
    }

    @Synchronized
    open fun clearUserContent() {
        log("Clear user content in VarCache")
        // 1. clear Var state to allow callback invocation when server values are downloaded
        val clientRegisteredVars = HashMap(vars)
        for (name in clientRegisteredVars.keys) {
            val `var` = vars[name]
            `var`?.clearStartFlag()
        }

        // 2. reset server values for previous user
        applyVariableDiffs(HashMap(), clientRegisteredVars)
        abVariants = ArrayList()

        // 3. reset data in shared prefs
        saveDiffsAsync()
    }

    @Synchronized
    open fun <T> getVariable(name: String?): Var<T>? {
        return JsonUtil.uncheckedCast(vars[name])
    }

    @VisibleForTesting
    internal fun getVariablesCount(): Int {
        return vars.size
    }

    @Synchronized
    open fun setGlobalCallbacksRunnable(runnable: Runnable?) {
        globalCallbacksRunnable = runnable
    }

    open fun filePathFromDisk(url: String?): String? {
        return FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger).cachedFilePath(url)
    }

    open fun fileVarUpdated(fileVar: Var<String>) {
        val url = fileVar.rawFileValue()
        val task = CTExecutorFactory.executors(instanceConfig).ioTask<Boolean>()
        task.addOnSuccessListener(OnSuccessListener { isCached ->
            if (isCached) {
                fileVar.triggerFileIsReady()
            } else {
                val list: MutableList<Pair<String, CtCacheType>> = ArrayList()
                list.add(Pair(url!!, CtCacheType.FILES))
                val completion: (Map<String, Boolean>) -> Unit = { fileVar.triggerFileIsReady() }
                fileResourcesRepoImpl.preloadFilesAndCache(list, completion)
            }
        })
        task.execute("isFileCached", Callable {
            url == null || FileResourceProvider.getInstance(variablesCtx, instanceConfig.logger).isFileCached(url)
        })
    }

    open fun variants(): List<Map<String, Any?>> {
        return abVariants
    }

    private companion object {
        private fun log(msg: String) {
            Logger.d("variables", msg)
        }

        private fun log(msg: String, t: Throwable) {
            Logger.d("variables", msg, t)
        }
    }
}
