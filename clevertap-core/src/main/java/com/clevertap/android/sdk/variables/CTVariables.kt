package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import org.json.JSONArray
import org.json.JSONObject

/**
 * Package Private class to not allow external access to working of Variables
 *
 * @author Ansh Sachdeva
 */
internal open class CTVariables(private val varCache: VarCache) {

    private var hasVarsRequestCompleted = false
    private var preRegisteredFilesDownloaded = false

    private val variablesChangedCallbacks: MutableList<VariablesChangedCallback> = ArrayList()
    private val oneTimeVariablesChangedCallbacks: MutableList<VariablesChangedCallback> = ArrayList()
    private val variablesChangedCallbacksNoDownloadsPending: MutableList<VariablesChangedCallback> = ArrayList()
    private val oneTimeVariablesChangedCallbacksNoDownloadsPending: MutableList<VariablesChangedCallback> =
        ArrayList()

    init {
        val triggerGlobalCallbacks = Runnable {
            synchronized(variablesChangedCallbacks) {
                for (callback in variablesChangedCallbacks) {
                    Utils.runOnUiThread(callback)
                }
            }
            synchronized(oneTimeVariablesChangedCallbacks) {
                for (callback in oneTimeVariablesChangedCallbacks) {
                    Utils.runOnUiThread(callback)
                }
                oneTimeVariablesChangedCallbacks.clear()
            }
        }
        this.varCache.setGlobalCallbacksRunnable(triggerGlobalCallbacks)
    }

    open fun init() {
        logD("init() called")
        varCache.loadDiffs { }
    }

    open fun handleVariableResponse(
        response: JSONObject?,
        fetchCallback: FetchVariablesCallback?
    ) {
        logD("handleVariableResponse() called with: response = [$response]")

        if (response == null) {
            handleVariableResponseError(fetchCallback)
        } else {
            handleVariableResponseSuccess(response, fetchCallback)
        }
    }

    open fun handleAbVariantsResponse(abVariants: JSONArray?) {
        logD("handleVariantsResponse() called with: abVariants  $abVariants")
        val list: List<Map<String, Any?>> = JsonUtil.listFromJsonFromDefault(abVariants)
        varCache.updateAbVariants(list)
    }

    open fun handleVariableResponseError(fetchCallback: FetchVariablesCallback?) {
        if (!hasVarsRequestCompleted()) {
            setHasVarsRequestCompleted(true)
            varCache.loadDiffsAndTriggerHandlers {
                triggerGlobalFilesCallbacks()
                preRegisteredFilesDownloaded = true
            }
        }
        fetchCallback?.onVariablesFetched(false)
    }

    private fun handleVariableResponseSuccess(
        response: JSONObject,
        fetchCallback: FetchVariablesCallback?
    ) {
        setHasVarsRequestCompleted(true)
        var variableDiffs: Map<String, Any?>? = JsonUtil.mapFromJson(response)
        variableDiffs = CTVariableUtils.convertFlatMapToNestedMaps(variableDiffs!!)
        varCache.updateDiffsAndTriggerHandlers(variableDiffs) {
            triggerGlobalFilesCallbacks()
            preRegisteredFilesDownloaded = true
        }
        fetchCallback?.onVariablesFetched(true)
    }

    /**
     * Triggers global callbacks for file download updates
     */
    private fun triggerGlobalFilesCallbacks() {
        synchronized(variablesChangedCallbacksNoDownloadsPending) {
            for (callback in variablesChangedCallbacksNoDownloadsPending) {
                Utils.runOnUiThread(callback)
            }
        }
        synchronized(oneTimeVariablesChangedCallbacksNoDownloadsPending) {
            for (callback in oneTimeVariablesChangedCallbacksNoDownloadsPending) {
                Utils.runOnUiThread(callback)
            }
            oneTimeVariablesChangedCallbacksNoDownloadsPending.clear()
        }
    }

    /**
     * clear current variable data.can be used during profile switch
     */
    open fun clearUserContent() {
        logD("Clear user content in CTVariables")
        setHasVarsRequestCompleted(false) // disable callbacks and wait until fetch is finished
        preRegisteredFilesDownloaded = false
        varCache.clearUserContent()
    }

    /**
     * Note: it is necessary to call the listeners immediately after user adds a listener using this function
     * because there is no guarantee that the newly added listener is going to be called later.
     *
     * we only receive variable data (and trigger handlers) on certain event calls that happens asynchronously,
     * so if client registers the listeners after such events have triggered,
     * they should still be able to get those previously updated values
     */
    open fun addVariablesChangedCallback(callback: VariablesChangedCallback) {
        synchronized(variablesChangedCallbacks) {
            variablesChangedCallbacks.add(callback)
        }

        if (hasVarsRequestCompleted) {
            callback.variablesChanged()
        }
    }

    /**
     * Note: Following the similar logic as mentioned in the comment on [addVariablesChangedCallback],
     * if a user adds a OneTimeVariablesChangedHandler using this function AFTER the variable data is received,
     * (and one time handlers already triggered), the user will not have their newly
     * added handlers triggered ever. therefore, it is necessary to trigger the user's handlers
     * immediately once this function is called
     */
    open fun addOneTimeVariablesChangedCallback(callback: VariablesChangedCallback) {
        if (hasVarsRequestCompleted) {
            callback.variablesChanged()
        } else {
            synchronized(oneTimeVariablesChangedCallbacks) {
                oneTimeVariablesChangedCallbacks.add(callback)
            }
        }
    }

    /**
     * Note: it is necessary to call the listeners immediately after user adds a listener using this function
     * because there is no guarantee that the newly added listener is going to be called later.
     *
     * we only receive variable data (and trigger handlers) on certain event calls that happens asynchronously,
     * so if client registers the listeners after such events have triggered,
     * they should still be able to get those previously updated values
     */
    open fun onVariablesChangedAndNoDownloadsPending(callback: VariablesChangedCallback) {
        synchronized(variablesChangedCallbacksNoDownloadsPending) {
            variablesChangedCallbacksNoDownloadsPending.add(callback)
        }

        if (preRegisteredFilesDownloaded) {
            callback.variablesChanged()
        }
    }

    /**
     * Note: Following the similar logic as mentioned in the comment on [addVariablesChangedCallback],
     * if a user adds a OneTimeVariablesChangedHandler using this function AFTER the variable data is received,
     * (and one time handlers already triggered), the user will not have their newly
     * added handlers triggered ever. therefore, it is necessary to trigger the user's handlers
     * immediately once this function is called
     */
    // change name to oncePreSdkInitRegisteredVariablesChangedAndNoDownloadsPending
    open fun onceVariablesChangedAndNoDownloadsPending(callback: VariablesChangedCallback) {
        if (preRegisteredFilesDownloaded) {
            callback.variablesChanged()
        } else {
            synchronized(oneTimeVariablesChangedCallbacksNoDownloadsPending) {
                oneTimeVariablesChangedCallbacksNoDownloadsPending.add(callback)
            }
        }
    }

    open fun removeVariablesChangedCallback(callback: VariablesChangedCallback) {
        synchronized(variablesChangedCallbacks) {
            variablesChangedCallbacks.remove(callback)
        }
    }

    open fun removeOneTimeVariablesChangedHandler(callback: VariablesChangedCallback) {
        synchronized(oneTimeVariablesChangedCallbacks) {
            oneTimeVariablesChangedCallbacks.remove(callback)
        }
    }

    open fun removeAllVariablesChangedCallbacks() {
        synchronized(variablesChangedCallbacks) {
            variablesChangedCallbacks.clear()
        }
    }

    open fun removeAllOneTimeVariablesChangedCallbacks() {
        synchronized(oneTimeVariablesChangedCallbacks) {
            oneTimeVariablesChangedCallbacks.clear()
        }
    }

    open fun hasVarsRequestCompleted(): Boolean {
        return hasVarsRequestCompleted
    }

    open fun setHasVarsRequestCompleted(completed: Boolean) {
        hasVarsRequestCompleted = completed
    }

    internal fun getVarCache(): VarCache {
        return varCache
    }

    private companion object {
        private fun logD(msg: String) {
            Logger.d("variables", msg)
        }
    }
}
