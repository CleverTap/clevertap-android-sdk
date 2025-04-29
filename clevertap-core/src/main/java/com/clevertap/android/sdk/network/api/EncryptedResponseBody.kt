package com.clevertap.android.sdk.network.api

import org.json.JSONObject

data class EncryptedResponseBody(
    val encryptedPayload: String,
    val iv: String
) {
    companion object {
        fun fromJsonString(json: String) : EncryptedResponseBody {
            val jsonObject = JSONObject(json)
            return EncryptedResponseBody(
                jsonObject.getString("encryptedPayload"),
                jsonObject.getString("iv")
            )
        }
    }
}