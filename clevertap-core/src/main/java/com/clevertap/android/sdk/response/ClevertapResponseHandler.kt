package com.clevertap.android.sdk.response

import android.content.Context
import org.json.JSONObject

internal class ClevertapResponseHandler(
    val context: Context,
    val responses: List<CleverTapResponse>,
) {

    fun handleResponse(
        isFullResponse: Boolean,
        bodyJson: JSONObject?,
        bodyString: String,
        isUserSwitching: Boolean
    ) = handleResponse(isFullResponse, bodyJson, bodyString, isUserSwitching, source = CTResponseSource.A1)

    fun handleResponse(
        isFullResponse: Boolean,
        bodyJson: JSONObject?,
        bodyString: String,
        isUserSwitching: Boolean,
        source: CTResponseSource
    ) {
        // Request-scoped flags travel as an immutable argument, not shared mutable decorator fields —
        // so concurrent /a1 and /content processing can't race on them.
        val responseContext = ResponseContext(isFullResponse, isUserSwitching, source)
        responses
            .filter { decorator -> !isUserSwitching || decorator.runsDuringUserSwitch() }
            .forEach { decorator ->
                decorator.processResponse(bodyJson, bodyString, context, responseContext)
            }
    }
}