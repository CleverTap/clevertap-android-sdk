package com.clevertap.android.sdk.features

import org.json.JSONObject

internal interface CleverTapFeature {
    fun handleApiData(response: JSONObject)
}