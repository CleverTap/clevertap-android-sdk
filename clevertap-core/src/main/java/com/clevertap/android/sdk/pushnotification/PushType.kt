package com.clevertap.android.sdk.pushnotification

import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

/**
 * Describes a push delivery channel: its [type], the preference key its token is stored under
 * ([tokenPrefKey]), the CleverTap provider class ([ctProviderClassName]) and the messaging SDK class
 * whose presence enables it ([messagingSDKClassName]).
 *
 * The two class names are nullable: wrapper SDKs build a [PushType] from maps where they may be absent.
 */
open class PushType(
    type: String,
    prefKey: String,
    className: String?,
    messagingSDKClassName: String?
) {

    open val ctProviderClassName: String? = className

    open val messagingSDKClassName: String? = messagingSDKClassName

    open val tokenPrefKey: String = prefKey

    open val type: String = type

    override fun toString(): String {
        return " [PushType:$type] "
    }

    // Method to convert PushType to JSONObject
    open fun toJSONObject(): JSONObject? {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("ctProviderClassName", ctProviderClassName)
            jsonObject.put("messagingSDKClassName", messagingSDKClassName)
            jsonObject.put("tokenPrefKey", tokenPrefKey)
            jsonObject.put("type", type)
        } catch (e: JSONException) {
            return null
        }
        return jsonObject
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PushType) {
            return false
        }
        return ctProviderClassName == other.ctProviderClassName &&
                messagingSDKClassName == other.messagingSDKClassName &&
                tokenPrefKey == other.tokenPrefKey &&
                type == other.type
    }

    override fun hashCode(): Int {
        return Objects.hash(ctProviderClassName, messagingSDKClassName, tokenPrefKey, type)
    }

    companion object {

        // Static method to convert JSONObject to PushType
        @JvmStatic
        fun fromJSONObject(jsonObject: JSONObject?): PushType? {
            if (jsonObject == null) {
                return null
            }
            return try {
                val ctProviderClassName = jsonObject.getString("ctProviderClassName")
                val messagingSDKClassName = jsonObject.getString("messagingSDKClassName")
                val tokenPrefKey = jsonObject.getString("tokenPrefKey")
                val type = jsonObject.getString("type")
                PushType(type, tokenPrefKey, ctProviderClassName, messagingSDKClassName)
            } catch (e: JSONException) {
                null
            }
        }
    }
}
