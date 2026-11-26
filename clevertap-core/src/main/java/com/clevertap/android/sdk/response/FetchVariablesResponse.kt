package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import org.json.JSONObject

internal open class FetchVariablesResponse(
    private val config: CleverTapInstanceConfig,
    private val controllerManager: ControllerManager,
    private val callbackMgr: BaseCallbackManager
) : CleverTapResponseDecorator() {

    private fun logD(m: String) = Logger.d("variables", m)

    // logs at debug level, as it always has
    private fun logI(m: String) = Logger.d("variables", m)

    private fun logI(m: String, t: Throwable) = Logger.i("variables", m, t)

    // variables are re-fetched for the new user after the switch
    override fun runsDuringUserSwitch(): Boolean = false

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        logI("Processing Variable response...")
        logD("processResponse() called with: response = [$jsonBody], stringBody = [$stringBody], context = [$context]")

        if (config.isAnalyticsOnly) {
            logI("CleverTap instance is configured to analytics only, not processing Variable response")
            return
        }

        if (jsonBody == null) {
            logI("Can't parse Variable Response, JSON response object is null")
            return
        }

        extractAbVariantsFromServer(jsonBody)
        extractVarsFromServer(jsonBody)
    }

    private fun extractVarsFromServer(response: JSONObject) {
        val varsKey = Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY

        if (!response.has(varsKey)) {
            logI("JSON object doesn't contain the $varsKey key")
            return
        }

        try {
            logI("Processing Request Variables response")

            val kvJson = response.getJSONObject(varsKey)

            val ctVariables = controllerManager.ctVariables
            if (ctVariables != null) {
                ctVariables.handleVariableResponse(kvJson, callbackMgr.fetchVariablesCallback)
                callbackMgr.fetchVariablesCallback = null
            } else {
                logI("Can't parse Variable Response, CTVariables is null")
            }
        } catch (t: Throwable) {
            logI("Failed to parse response", t)
        }
    }

    private fun extractAbVariantsFromServer(response: JSONObject) {
        val variantsKey = Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY

        if (!response.has(variantsKey)) {
            logI("JSON object doesn't contain the $variantsKey key")
            return
        }

        try {
            logI("Processing Variants response")

            val abVariantsJson = response.optJSONArray(variantsKey)
            if (abVariantsJson == null) {
                logI("Not handling AB variants cache as response is null")
                return
            }

            val ctVariables = controllerManager.ctVariables
            if (ctVariables != null) {
                ctVariables.handleAbVariantsResponse(abVariantsJson)
            } else {
                logI("Can't parse Variant Response, CTVariables is null")
            }
        } catch (t: Throwable) {
            logI("Failed to parse variants response", t)
        }
    }
}
