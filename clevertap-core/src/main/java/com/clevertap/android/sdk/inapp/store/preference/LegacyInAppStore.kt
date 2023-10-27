package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.concatIfNotNull
import com.clevertap.android.sdk.store.preference.ICTPreference
import org.json.JSONArray
import org.json.JSONException

class LegacyInAppStore(private val ctPreference: ICTPreference, accountId: String) {

    private val inAppKey = Constants.INAPP_KEY.concatIfNotNull(accountId, ":")

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