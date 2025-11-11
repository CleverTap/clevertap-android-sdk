package com.clevertap.android.sdk.features

import org.json.JSONObject

internal interface CleverTapFeature {
    fun handleApiData(response: JSONObject) = handleApiData(
        response = response,
        isFullResponse = false,
        isUserSwitching = false
    )
    fun handleApiData(response: JSONObject, isFullResponse: Boolean, isUserSwitching: Boolean)
}