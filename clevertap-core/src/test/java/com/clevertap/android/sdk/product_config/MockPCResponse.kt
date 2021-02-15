package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

class MockPCResponse {

    fun getFetchedConfig(): HashMap<String, Any> {
        val fetchedConfig: HashMap<String, Any> = HashMap()
        fetchedConfig.put("fetched_str", "This is fetched string")
        fetchedConfig.put("fetched_long", 333333L)
        fetchedConfig.put("fetched_double", 44444.4444)
        fetchedConfig.put("fetched_bool", true)
        return fetchedConfig
    }

    fun getDefaultConfig(): HashMap<String, Any> {
        val defaultConfig: HashMap<String, Any> = HashMap()
        defaultConfig.put("def_str", "This is def_string")
        defaultConfig.put("def_long", 11111L)
        defaultConfig.put("def_double", 2222.2222)
        defaultConfig.put("def_bool", false)
        return defaultConfig
    }

    fun getMockPCResponse(): JSONObject {
        val response = JSONObject()
        val array = JSONArray()
        for (entry in getFetchedConfig()) {
            val jsonObject = JSONObject()
            jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_KEY, entry.key)
            jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_JSON_KEY_FOR_VALUE, entry.value)
            array.put(jsonObject)
        }
        response.put(Constants.KEY_KV, array)
        response.put(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP, (System.currentTimeMillis() / 1000).toInt())
        return response
    }
}