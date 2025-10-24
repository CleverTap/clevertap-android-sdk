package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import org.json.JSONObject

internal interface CleverTapFeature {

    fun coreContract(coreContract: CoreContract)

    fun handleApiData(response: JSONObject, stringBody: String, context: Context)
}