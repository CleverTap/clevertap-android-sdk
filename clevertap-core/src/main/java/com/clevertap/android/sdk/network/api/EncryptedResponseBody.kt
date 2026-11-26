package com.clevertap.android.sdk.network.api

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EncryptedResponseBody(
    val encryptedPayload: String,
    val iv: String
) {
    companion object {
        fun fromJsonString(json: String) : EncryptedResponseBody {
            val jsonObject = JSONObject(json)
            return EncryptedResponseBody(
                jsonObject.getString("itp"),
                jsonObject.getString("itv")
            )
        }
    }
}
