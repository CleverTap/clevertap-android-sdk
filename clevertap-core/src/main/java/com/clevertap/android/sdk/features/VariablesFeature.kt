package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.response.FetchVariablesResponse
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import org.json.JSONObject

internal data class VariablesFeature(
    val cTVariables: CTVariables,
    val varCache: VarCache,
    val parser: Parser,
    val variablesRepository: VariablesRepo,
    val fetchVariablesResponse: FetchVariablesResponse
) : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var fetchVariablesCallback: FetchVariablesCallback? = null

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
    ) {
        fetchVariablesResponse.processResponse(response, stringBody, context)
    }

    fun invokeCallbacksForNetworkError() {
        cTVariables.handleVariableResponseError(fetchVariablesCallback)
        fetchVariablesCallback = null
    }

}
