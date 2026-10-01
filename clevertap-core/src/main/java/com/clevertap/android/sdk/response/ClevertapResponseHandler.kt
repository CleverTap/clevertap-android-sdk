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
    ) {
        if (isUserSwitching) {
            responses
                .filterNot { decorator ->
                    decorator is InboxResponse || decorator is FetchVariablesResponse
                }
                .forEach { decorator ->
                    decorator.isFullResponse = isFullResponse
                    // DisplayUnitResponse and InAppResponse handle the user-switch flag internally
                    // (ND ingests meta but skips content) rather than being filtered out wholesale.
                    when (decorator) {
                        is InAppResponse -> decorator.processResponse(bodyJson, bodyString, context, true)
                        is DisplayUnitResponse -> decorator.processResponse(bodyJson, bodyString, context, true)
                        else -> decorator.processResponse(bodyJson, bodyString, context)
                    }
                }
        } else {
            responses.forEach { decorator ->
                decorator.isFullResponse = isFullResponse
                decorator.processResponse(bodyJson, bodyString, context)
            }
        }
    }
}