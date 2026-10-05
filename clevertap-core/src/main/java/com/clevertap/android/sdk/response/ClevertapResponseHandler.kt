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
        responses
            .filter { decorator -> !isUserSwitching || decorator.runsDuringUserSwitch() }
            .forEach { decorator ->
                decorator.responseSource = source
                // InAppResponse is the only consumer of isFullResponse, so it's passed as a call
                // argument rather than a shared field. TODO(refactor): responseSource still rides a
                // shared mutable field (read by InAppResponse/ContentFetchResponse/DisplayUnitResponse);
                // fold it + isUserSwitching into an immutable ResponseContext to remove it too.
                if (decorator is InAppResponse) {
                    decorator.processResponse(bodyJson, bodyString, context, isUserSwitching, isFullResponse)
                } else {
                    decorator.processResponse(bodyJson, bodyString, context)
                }
            }
    }
}