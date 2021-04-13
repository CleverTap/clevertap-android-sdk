package com.clevertap.android.sdk.displayunits.model

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.displayunits.CTDisplayUnitType
import org.json.JSONArray
import org.json.JSONObject

class MockCleverTapDisplayUnit {

    fun getMockResponse(noItems: Int): JSONArray {
        val jsonArray = JSONArray()
        for (i in 1..noItems)
            jsonArray.put(MockCleverTapDisplayUnit().getAUnit())
        return jsonArray
    }

    fun getAUnit(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.NOTIFICATION_ID_TAG, "mock-notification-id")
        jsonObject.put(Constants.KEY_TYPE, CTDisplayUnitType.SIMPLE.type)
        jsonObject.put(Constants.KEY_BG, "mock-unit-bg")
        val contentArray = JSONArray()
        for (i in 1..3) {
            contentArray.put(MockDisplayUnitContent().getContent())
        }
        jsonObject.put(Constants.KEY_CONTENT, contentArray)

        val customKV = JSONObject()
        customKV.put("k1","v1")
        customKV.put("k2","v2")
        jsonObject.put(Constants.KEY_CUSTOM_KV, customKV)
        return jsonObject
    }
}