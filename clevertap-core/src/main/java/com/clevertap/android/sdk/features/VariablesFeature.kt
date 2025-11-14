package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.response.FetchVariablesResponse
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import org.json.JSONObject

/**
 * Variables feature for managing dynamic variables and A/B testing
 * Handles variable definitions, fetching, and updates
 */
internal class VariablesFeature(
    private val storeRegistry: StoreRegistry,
    private val dbEncryptionHandler: DBEncryptionHandler,
    private var fetchVariablesCallback: FetchVariablesCallback? = null
) : CleverTapFeature {

    lateinit var coreContract: CoreContract

    // Lazy-initialized Variables dependencies (initialized after coreContract is set)
    val variablesRepository: VariablesRepo by lazy {
        VariablesRepo(
            context = coreContract.context(),
            accountId = coreContract.config().accountId,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    private val fileResourcesRepo by lazy {
        FileResourcesRepoFactory.createFileResourcesRepo(
            context = coreContract.context(),
            logger = coreContract.logger(),
            storeRegistry = storeRegistry
        )
    }

    val varCache: VarCache by lazy {
        VarCache(
            coreContract.config(),
            coreContract.context(),
            fileResourcesRepo,
            variablesRepository
        )
    }

    val cTVariables: CTVariables by lazy {
        CTVariables(varCache)
    }

    val parser: Parser by lazy {
        Parser(cTVariables)
    }

    val fetchVariablesResponse: FetchVariablesResponse by lazy {
        FetchVariablesResponse(coreContract.config(), cTVariables)
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
        fetchVariablesResponse.processResponse(response)
    }

    fun invokeCallbacksForNetworkError() {
        cTVariables.handleVariableResponseError(fetchVariablesCallback)
        fetchVariablesCallback = null
    }

    fun reset() {
        cTVariables.clearUserContent()
    }

    // ========== PUBLIC API FACADES ==========
    // These methods provide direct delegation from CleverTapAPI to Variables functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Define a new variable
     */
    fun <T> defineVariable(name: String?, defaultValue: T): com.clevertap.android.sdk.variables.Var<T> {
        return com.clevertap.android.sdk.variables.Var.define(name, defaultValue, cTVariables)
    }

    /**
     * Define a new file variable
     */
    fun defineFileVariable(name: String?): com.clevertap.android.sdk.variables.Var<String> {
        return com.clevertap.android.sdk.variables.Var.define(
            name,
            null,
            com.clevertap.android.sdk.variables.CTVariableUtils.FILE,
            cTVariables
        )
    }

    /**
     * Parse @Variable annotated fields from instances
     */
    fun parseVariables(vararg instances: Any?) {
        parser.parseVariables(*instances)
    }

    /**
     * Parse @Variable annotated static fields from classes
     */
    fun parseVariablesForClasses(vararg classes: Class<*>?) {
        parser.parseVariablesForClasses(*classes)
    }

    /**
     * Get a copy of the current value of a variable or a group
     */
    fun getVariableValue(name: String?): Any? {
        if (name == null) {
            return null
        }
        return varCache.getMergedValue(name)
    }

    /**
     * Get an instance of a variable or a group
     */
    fun <T> getVariable(name: String?): com.clevertap.android.sdk.variables.Var<T>? {
        if (name == null) {
            return null
        }
        return varCache.getVariable(name)
    }

    /**
     * Set the fetch variables callback
     */
    fun setFetchVariablesCallback(callback: FetchVariablesCallback?) {
        this.fetchVariablesCallback = callback
    }

    /**
     * Add a callback to be invoked when variables are changed
     */
    fun addVariablesChangedCallback(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.addVariablesChangedCallback(callback)
    }

    /**
     * Add a one-time callback to be invoked when variables are changed
     */
    fun addOneTimeVariablesChangedCallback(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.addOneTimeVariablesChangedCallback(callback)
    }

    /**
     * Remove a variables changed callback
     */
    fun removeVariablesChangedCallback(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.removeVariablesChangedCallback(callback)
    }

    /**
     * Remove a one-time variables changed callback
     */
    fun removeOneTimeVariablesChangedCallback(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.removeOneTimeVariablesChangedHandler(callback)
    }

    /**
     * Add callback for variables changed and no downloads pending
     */
    fun onVariablesChangedAndNoDownloadsPending(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.onVariablesChangedAndNoDownloadsPending(callback)
    }

    /**
     * Add one-time callback for variables changed and no downloads pending
     */
    fun onceVariablesChangedAndNoDownloadsPending(callback: com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback) {
        cTVariables.onceVariablesChangedAndNoDownloadsPending(callback)
    }

    /**
     * Remove all variables changed callbacks
     */
    fun removeAllVariablesChangedCallbacks() {
        cTVariables.removeAllVariablesChangedCallbacks()
    }

    /**
     * Remove all one-time variables changed callbacks
     */
    fun removeAllOneTimeVariablesChangedCallbacks() {
        cTVariables.removeAllOneTimeVariablesChangedCallbacks()
    }

    /**
     * Fetches variable values from server.
     * Note that SDK keeps only one registered callback, if you call that method again it would
     * override the callback.
     *
     * @param callback Callback instance to be invoked when fetching is done.
     */
    fun fetchVariables(callback: FetchVariablesCallback?) {
        if (coreContract.config().isAnalyticsOnly) {
            return
        }
        Logger.v("variables", "Fetching  variables")
        if (callback != null) {
            setFetchVariablesCallback(callback)
        }

        val event = AnalyticsFeature.fetchRequestAsJson(Constants.FETCH_TYPE_VARIABLES)
        coreContract.analytics().sendFetchEvent(event)
    }

    // ========== PUBLIC API FACADES END ==========
}
