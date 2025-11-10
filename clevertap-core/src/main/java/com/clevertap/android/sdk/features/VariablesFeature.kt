package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
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
    var fetchVariablesCallback: FetchVariablesCallback? = null
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
        response: JSONObject
    ) {
        fetchVariablesResponse.processResponse(response)
    }

    fun invokeCallbacksForNetworkError() {
        cTVariables.handleVariableResponseError(fetchVariablesCallback)
        fetchVariablesCallback = null
    }

    /**
     * Phase 1: Reset method moved from CoreState
     * Clears all user-specific variable content
     */
    fun reset() {
        cTVariables.clearUserContent()
    }

}
