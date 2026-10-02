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
                decorator.isFullResponse = isFullResponse
                decorator.responseSource = source
                // TODO(refactor): InAppResponse still needs isUserSwitching passed explicitly. Fold
                //  isFullResponse/responseSource/isUserSwitching into an immutable ResponseContext
                //  param so this concrete-type special-case and the field mutation above both go away.
                if (isUserSwitching && decorator is InAppResponse) {
                    decorator.processResponse(bodyJson, bodyString, context, true)
                } else {
                    decorator.processResponse(bodyJson, bodyString, context)
                }
            }
    }
}