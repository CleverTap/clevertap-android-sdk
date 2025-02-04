package com.clevertap.android.sdk.network.api

import org.json.JSONObject

data class EncryptedSendQueueRequestBody(
    val encryptedPayload: String,
    val key: String,
    val keyVersion: String,
    val iv: String
) {

    fun toJsonString() : String {
        return JSONObject().apply {
            put("encryptedPayload", encryptedPayload)
            put("key", key)
            put("keyVersion", keyVersion)
            put("iv", iv)
        }.toString()
    }
}
