package com.clevertap.android.sdk

import org.json.JSONArray
import org.json.JSONException

class LegacyInAppStore(private val ctPreference: ICTPreference, config: CleverTapInstanceConfig) {

    private val inAppKey = Constants.INAPP_KEY.concatIfNotNull(config.accountId, ":")

    fun storeInApps(inApps: JSONArray) {
        ctPreference.writeStringImmediate(inAppKey!!, inApps.toString())
    }

    fun readInApps(): JSONArray {
        val storedInApps = ctPreference.readString(inAppKey!!, "[]")
        return try {
            JSONArray(storedInApps)
        } catch (e: JSONException) {
            JSONArray()
        }
    }
}