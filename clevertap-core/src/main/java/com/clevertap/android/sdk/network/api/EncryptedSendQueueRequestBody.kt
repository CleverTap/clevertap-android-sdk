package com.clevertap.android.sdk.network.api

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EncryptedSendQueueRequestBody(
    val encryptedPayload: String,
    val key: String,
    val iv: String
) {

    companion object {
        private const val KEY_ENCRYPTED_PAYLOAD = "itp"
        private const val KEY_KEY = "itk"
        private const val KEY_IV = "itv"
    }

    fun toJsonString() : String {
        return JSONObject().apply {
            put(KEY_ENCRYPTED_PAYLOAD, encryptedPayload)
            put(KEY_KEY, key)
            put(KEY_IV, iv)
        }.toString()
    }
}
