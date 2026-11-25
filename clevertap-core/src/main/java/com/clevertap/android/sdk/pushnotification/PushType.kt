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

    private val _ctProviderClassName: String? = className

    open fun getCtProviderClassName(): String? = _ctProviderClassName

    /** Kotlin property syntax, equivalent to [getCtProviderClassName]. */
    val ctProviderClassName: String?
        @JvmSynthetic @JvmName("ktGetCtProviderClassName") inline get() = getCtProviderClassName()

    private val _messagingSDKClassName: String? = messagingSDKClassName

    open fun getMessagingSDKClassName(): String? = _messagingSDKClassName

    /** Kotlin property syntax, equivalent to [getMessagingSDKClassName]. */
    val messagingSDKClassName: String?
        @JvmSynthetic @JvmName("ktGetMessagingSDKClassName") inline get() = getMessagingSDKClassName()

    private val _tokenPrefKey: String = prefKey

    open fun getTokenPrefKey(): String = _tokenPrefKey

    /** Kotlin property syntax, equivalent to [getTokenPrefKey]. */
    val tokenPrefKey: String
        @JvmSynthetic @JvmName("ktGetTokenPrefKey") inline get() = getTokenPrefKey()

    private val _type: String = type

    open fun getType(): String = _type

    /** Kotlin property syntax, equivalent to [getType]. */
    val type: String
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    override fun toString(): String {
        return " [PushType:$_type] "
    }

    // Method to convert PushType to JSONObject
    open fun toJSONObject(): JSONObject? {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("ctProviderClassName", _ctProviderClassName)
            jsonObject.put("messagingSDKClassName", _messagingSDKClassName)
            jsonObject.put("tokenPrefKey", _tokenPrefKey)
            jsonObject.put("type", _type)
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
        return _ctProviderClassName == other._ctProviderClassName &&
                _messagingSDKClassName == other._messagingSDKClassName &&
                _tokenPrefKey == other._tokenPrefKey &&
                _type == other._type
    }

    override fun hashCode(): Int {
        return Objects.hash(_ctProviderClassName, _messagingSDKClassName, _tokenPrefKey, _type)
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
