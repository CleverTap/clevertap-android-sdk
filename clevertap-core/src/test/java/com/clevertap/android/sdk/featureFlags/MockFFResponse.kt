package com.clevertap.android.sdk.featureFlags

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY
import com.clevertap.android.sdk.product_config.CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE
import org.json.JSONArray
import org.json.JSONObject

class MockFFResponse {

    fun getFetchedFFConfig(): HashMap<String, Any> {
        val fetchedConfig: HashMap<String, Any> = HashMap()
        fetchedConfig.put("feature_A", true)
        fetchedConfig.put("feature_B", false)
        fetchedConfig.put("feature_C", false)
        fetchedConfig.put("feature_D", true)
        return fetchedConfig
    }

    fun getResponseJSON(): JSONObject {
        val response = JSONObject()
        val array = JSONArray()
        for (entry in getFetchedFFConfig()) {
            val jsonObject = JSONObject()
            jsonObject.put(PRODUCT_CONFIG_JSON_KEY_FOR_KEY, entry.key)
            jsonObject.put(PRODUCT_CONFIG_JSON_KEY_FOR_VALUE, entry.value)
            array.put(jsonObject)
        }
        response.put(Constants.KEY_KV, array)
        return response
    }
}