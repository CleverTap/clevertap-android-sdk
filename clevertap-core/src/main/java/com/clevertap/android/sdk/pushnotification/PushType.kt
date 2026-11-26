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
public open class PushType(
    type: String,
    prefKey: String,
    className: String?,
    messagingSDKClassName: String?
) {

    private val _ctProviderClassName: String? = className

    /**
     * Returns the fully qualified name of the CleverTap push provider class for this channel.
     *
     * @return the provider class name, or `null` if it was not supplied
     */
    public open fun getCtProviderClassName(): String? = _ctProviderClassName

    /** Kotlin property syntax, equivalent to [getCtProviderClassName]. */
    public val ctProviderClassName: String?
        @JvmSynthetic @JvmName("ktGetCtProviderClassName") inline get() = getCtProviderClassName()

    private val _messagingSDKClassName: String? = messagingSDKClassName

    /**
     * Returns the fully qualified name of a messaging SDK class whose presence on the classpath
     * enables this channel.
     *
     * @return the messaging SDK class name, or `null` if it was not supplied
     */
    public open fun getMessagingSDKClassName(): String? = _messagingSDKClassName

    /** Kotlin property syntax, equivalent to [getMessagingSDKClassName]. */
    public val messagingSDKClassName: String?
        @JvmSynthetic @JvmName("ktGetMessagingSDKClassName") inline get() = getMessagingSDKClassName()

    private val _tokenPrefKey: String = prefKey

    /**
     * Returns the preference key under which the SDK stores this channel's push token.
     *
     * @return the token preference key
     */
    public open fun getTokenPrefKey(): String = _tokenPrefKey

    /** Kotlin property syntax, equivalent to [getTokenPrefKey]. */
    public val tokenPrefKey: String
        @JvmSynthetic @JvmName("ktGetTokenPrefKey") inline get() = getTokenPrefKey()

    private val _type: String = type

    /**
     * Returns the identifier of this push channel, for example `fcm`.
     *
     * @return the push type identifier
     */
    public open fun getType(): String = _type

    /** Kotlin property syntax, equivalent to [getType]. */
    public val type: String
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    override fun toString(): String = " [PushType:$_type] "

    // Method to convert PushType to JSONObject
    /**
     * Serialises this push type to JSON with the keys `type`, `tokenPrefKey`, `ctProviderClassName`
     * and `messagingSDKClassName` (a `null` class name leaves its key out).
     *
     * @return the JSON form, or `null` if it could not be built
     * @see fromJSONObject
     */
    public open fun toJSONObject(): JSONObject? = try {
        JSONObject().apply {
            put("ctProviderClassName", _ctProviderClassName)
            put("messagingSDKClassName", _messagingSDKClassName)
            put("tokenPrefKey", _tokenPrefKey)
            put("type", _type)
        }
    } catch (e: JSONException) {
        null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is PushType &&
                _ctProviderClassName == other._ctProviderClassName &&
                _messagingSDKClassName == other._messagingSDKClassName &&
                _tokenPrefKey == other._tokenPrefKey &&
                _type == other._type
    }

    override fun hashCode(): Int =
        Objects.hash(_ctProviderClassName, _messagingSDKClassName, _tokenPrefKey, _type)

    public companion object {

        // Static method to convert JSONObject to PushType
        /**
         * Parses a push type from the JSON produced by [toJSONObject].
         *
         * @param jsonObject the JSON to parse
         * @return the push type, or `null` if [jsonObject] is `null` or any of the four keys is
         * missing
         */
        @JvmStatic
        public fun fromJSONObject(jsonObject: JSONObject?): PushType? {
            jsonObject ?: return null
            return try {
                PushType(
                    type = jsonObject.getString("type"),
                    prefKey = jsonObject.getString("tokenPrefKey"),
                    className = jsonObject.getString("ctProviderClassName"),
                    messagingSDKClassName = jsonObject.getString("messagingSDKClassName")
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}
