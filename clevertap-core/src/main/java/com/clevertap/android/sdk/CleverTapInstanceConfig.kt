package com.clevertap.android.sdk

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

import com.clevertap.android.sdk.Constants.IdentityType
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.login.LoginConstants
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getDefaultPushTypes
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.android.sdk.utils.CTJsonConverter.toArray

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Arrays

open class CleverTapInstanceConfig : Parcelable {

    private var _accountId: String? = null
    private var _accountRegion: String? = null
    private var _accountToken: String? = null
    private var _proxyDomain: String? = null
    private var _spikyProxyDomain: String? = null
    private var _customHandshakeDomain: String? = null
    private val _pushTypes: ArrayList<PushType> = getDefaultPushTypes()
    private var _analyticsOnly = false
    private var _backgroundSync = false
    private var _beta = false
    private var _createdPostAppLaunch = false
    private var _debugLevel = 0
    private var _disableAppLaunchedEvent = false
    private var _enableCustomCleverTapId = false
    private var _fcmSenderId: String? = null
    private var _isDefaultInstance = false
    private var _logger: Logger? = null
    private var _packageName: String? = null
    private var _personalization = false
    private var _identityKeys: Array<String>? = Constants.NULL_STRING_ARRAY
    private var _sslPinning = false
    private var _useGoogleAdId = false
    private var _encryptionLevel = 0
    private var _encryptionInTransit: String? = null

    internal constructor(config: CleverTapInstanceConfig) {
        this._accountId = config._accountId
        this._accountToken = config._accountToken
        this._accountRegion = config._accountRegion
        this._proxyDomain = config._proxyDomain
        this._spikyProxyDomain = config._spikyProxyDomain
        this._customHandshakeDomain = config._customHandshakeDomain
        this._isDefaultInstance = config._isDefaultInstance
        this._analyticsOnly = config._analyticsOnly
        this._personalization = config._personalization
        this._debugLevel = config._debugLevel
        this._logger = config._logger
        this._useGoogleAdId = config._useGoogleAdId
        this._disableAppLaunchedEvent = config._disableAppLaunchedEvent
        this._createdPostAppLaunch = config._createdPostAppLaunch
        this._sslPinning = config._sslPinning
        this._backgroundSync = config._backgroundSync
        this._enableCustomCleverTapId = config._enableCustomCleverTapId
        this._fcmSenderId = config._fcmSenderId
        this._packageName = config._packageName
        this._beta = config._beta
        this._identityKeys = config._identityKeys
        this._encryptionLevel = config._encryptionLevel
        for (pushType in config._pushTypes) {
            addPushType(pushType)
        }
        this._encryptionInTransit = config._encryptionInTransit
    }

    private constructor(
        manifest: ManifestInfo,
        accountId: String?,
        accountToken: String?,
        accountRegion: String?,
        isDefault: Boolean
    ) {
        this._accountId = accountId
        this._accountToken = accountToken
        this._accountRegion = accountRegion
        this._isDefaultInstance = isDefault
        this._analyticsOnly = false
        this._personalization = true
        this._debugLevel = CleverTapAPI.LogLevel.INFO.intValue()
        this._logger = Logger(this._debugLevel)
        this._createdPostAppLaunch = false

        this._useGoogleAdId = manifest.useGoogleAdId()
        this._disableAppLaunchedEvent = manifest.isAppLaunchedDisabled
        this._sslPinning = manifest.isSSLPinningEnabled
        this._backgroundSync = manifest.isBackgroundSync
        this._fcmSenderId = manifest.fcmSenderId
        this._packageName = manifest.packageName
        this._enableCustomCleverTapId = manifest.useCustomId()
        this._beta = manifest.enableBeta()
        /*
         * For default instance, use manifest meta, otherwise use from setter field
         */
        if (_isDefaultInstance) {
            this._encryptionLevel = manifest.encryptionLevel
            _identityKeys = manifest.profileKeys
            log(
                LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys from Manifest: " + Arrays
                    .toString(_identityKeys)
            )
        } else {
            this._encryptionLevel = 0
        }
        buildPushProvidersFromManifest(manifest)

        val fromManifest = manifest.encryptionInTransit
        this._encryptionInTransit = fromManifest ?: "0"
    }

    private fun buildPushProvidersFromManifest(manifest: ManifestInfo) {
        try {
            val provider1 = manifest.vendorOneProvider
            if (provider1 != null) {
                val splits = provider1.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splits.size == 4) {
                    val pushType = PushType(splits[0].trim(), splits[1].trim(), splits[2].trim(), splits[3].trim())
                    addPushType(pushType)
                }
            }
            val provider2 = manifest.vendorTwoProvider
            if (provider2 != null) {
                val splits = provider2.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splits.size == 4) {
                    val pushType = PushType(splits[0].trim(), splits[1].trim(), splits[2].trim(), splits[3].trim())
                    addPushType(pushType)
                }
            }
        } catch (e: Exception) {
            Logger.v("There was some problem in loading push providers from manifest")
        }
    }

    @Throws(Throwable::class)
    private constructor(jsonString: String?) {
        try {
            val configJsonObject = JSONObject(jsonString)
            if (configJsonObject.has(KEY_ACCOUNT_ID)) {
                this._accountId = configJsonObject.getString(KEY_ACCOUNT_ID)
            }
            if (configJsonObject.has(KEY_ACCOUNT_TOKEN)) {
                this._accountToken = configJsonObject.getString(KEY_ACCOUNT_TOKEN)
            }
            if (configJsonObject.has(KEY_PROXY_DOMAIN)) {
                this._proxyDomain = configJsonObject.getString(KEY_PROXY_DOMAIN)
            }
            if (configJsonObject.has(KEY_SPIKY_PROXY_DOMAIN)) {
                this._spikyProxyDomain = configJsonObject.getString(KEY_SPIKY_PROXY_DOMAIN)
            }
            if (configJsonObject.has(KEY_CUSTOM_HANDSHAKE_DOMAIN)) {
                this._customHandshakeDomain = configJsonObject.optString(KEY_CUSTOM_HANDSHAKE_DOMAIN, null)
            }
            if (configJsonObject.has(KEY_ACCOUNT_REGION)) {
                this._accountRegion = configJsonObject.getString(KEY_ACCOUNT_REGION)
            }
            if (configJsonObject.has(KEY_ANALYTICS_ONLY)) {
                this._analyticsOnly = configJsonObject.getBoolean(KEY_ANALYTICS_ONLY)
            }
            if (configJsonObject.has(KEY_DEFAULT_INSTANCE)) {
                this._isDefaultInstance = configJsonObject.getBoolean(KEY_DEFAULT_INSTANCE)
            }
            if (configJsonObject.has(KEY_USE_GOOGLE_AD_ID)) {
                this._useGoogleAdId = configJsonObject.getBoolean(KEY_USE_GOOGLE_AD_ID)
            }
            if (configJsonObject.has(KEY_DISABLE_APP_LAUNCHED)) {
                this._disableAppLaunchedEvent = configJsonObject.getBoolean(KEY_DISABLE_APP_LAUNCHED)
            }
            if (configJsonObject.has(KEY_PERSONALIZATION)) {
                this._personalization = configJsonObject.getBoolean(KEY_PERSONALIZATION)
            }
            if (configJsonObject.has(KEY_DEBUG_LEVEL)) {
                this._debugLevel = configJsonObject.getInt(KEY_DEBUG_LEVEL)
            }
            this._logger = Logger(this._debugLevel)

            if (configJsonObject.has(KEY_PACKAGE_NAME)) {
                this._packageName = configJsonObject.getString(KEY_PACKAGE_NAME)
            }
            if (configJsonObject.has(KEY_CREATED_POST_APP_LAUNCH)) {
                this._createdPostAppLaunch = configJsonObject.getBoolean(KEY_CREATED_POST_APP_LAUNCH)
            }
            if (configJsonObject.has(KEY_SSL_PINNING)) {
                this._sslPinning = configJsonObject.getBoolean(KEY_SSL_PINNING)
            }
            if (configJsonObject.has(KEY_BACKGROUND_SYNC)) {
                this._backgroundSync = configJsonObject.getBoolean(KEY_BACKGROUND_SYNC)
            }
            if (configJsonObject.has(KEY_ENABLE_CUSTOM_CT_ID)) {
                this._enableCustomCleverTapId = configJsonObject.getBoolean(KEY_ENABLE_CUSTOM_CT_ID)
            }
            if (configJsonObject.has(KEY_FCM_SENDER_ID)) {
                this._fcmSenderId = configJsonObject.getString(KEY_FCM_SENDER_ID)
            }
            if (configJsonObject.has(KEY_BETA)) {
                this._beta = configJsonObject.getBoolean(KEY_BETA)
            }
            if (configJsonObject.has(KEY_IDENTITY_TYPES)) {
                @Suppress("UNCHECKED_CAST")
                this._identityKeys = toArray<Any>(configJsonObject.getJSONArray(KEY_IDENTITY_TYPES)) as Array<String>
            }
            if (configJsonObject.has(KEY_ENCRYPTION_LEVEL)) {
                this._encryptionLevel = configJsonObject.getInt(KEY_ENCRYPTION_LEVEL)
            }
            if (configJsonObject.has(KEY_PUSH_TYPES)) {
                val pushTypesArray = configJsonObject.getJSONArray(KEY_PUSH_TYPES)
                for (i in 0 until pushTypesArray.length()) {
                    val pushTypeJo = pushTypesArray.getJSONObject(i)
                    val pushType = PushType.fromJSONObject(pushTypeJo)
                    if (pushType != null) {
                        addPushType(pushType)
                    }
                }
            }
            this._encryptionInTransit = configJsonObject.optString(KEY_ENCRYPTION_IN_TRANSIT, "0")
        } catch (t: Throwable) {
            Logger.v("Error constructing CleverTapInstanceConfig from JSON: $jsonString: ", t.cause)
            throw (t)
        }
    }

    private constructor(`in`: Parcel) {
        _accountId = `in`.readString()
        _accountToken = `in`.readString()
        _accountRegion = `in`.readString()
        _proxyDomain = `in`.readString()
        _spikyProxyDomain = `in`.readString()
        _customHandshakeDomain = `in`.readString()
        _analyticsOnly = `in`.readByte().toInt() != 0x00
        _isDefaultInstance = `in`.readByte().toInt() != 0x00
        _useGoogleAdId = `in`.readByte().toInt() != 0x00
        _disableAppLaunchedEvent = `in`.readByte().toInt() != 0x00
        _personalization = `in`.readByte().toInt() != 0x00
        _debugLevel = `in`.readInt()
        _createdPostAppLaunch = `in`.readByte().toInt() != 0x00
        _sslPinning = `in`.readByte().toInt() != 0x00
        _backgroundSync = `in`.readByte().toInt() != 0x00
        _enableCustomCleverTapId = `in`.readByte().toInt() != 0x00
        _fcmSenderId = `in`.readString()
        _packageName = `in`.readString()
        _logger = Logger(_debugLevel)
        _beta = `in`.readByte().toInt() != 0x00
        _identityKeys = `in`.createStringArray()
        _encryptionLevel = `in`.readInt()
        _encryptionInTransit = `in`.readString()
        try {
            val allowedTypesJsonArray = JSONArray(`in`.readString())
            for (i in 0 until allowedTypesJsonArray.length()) {
                val pushType = PushType.fromJSONObject(allowedTypesJsonArray.getJSONObject(i))
                if (pushType != null) {
                    addPushType(pushType)
                }
            }
        } catch (e: JSONException) {
            Logger.v("Error in loading push providers from parcel, using firebase")
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    open fun enablePersonalization(enablePersonalization: Boolean) {
        this._personalization = enablePersonalization
    }

    /**
     * The CleverTap account ID. Every public factory rejects a null account ID, so this is non-null
     * for any config created through the public API.
     */
    open fun getAccountId(): String = _accountId!!

    /** Kotlin property syntax, equivalent to [getAccountId]. */
    val accountId: String
        @JvmSynthetic @JvmName("ktGetAccountId") inline get() = getAccountId()

    open fun getAccountRegion(): String? = _accountRegion

    /** Kotlin property syntax, equivalent to [getAccountRegion]. */
    val accountRegion: String?
        @JvmSynthetic @JvmName("ktGetAccountRegion") inline get() = getAccountRegion()

    /**
     * The CleverTap account token. Every public factory rejects a null token, so this is non-null
     * for any config created through the public API.
     */
    open fun getAccountToken(): String = _accountToken!!

    /** Kotlin property syntax, equivalent to [getAccountToken]. */
    val accountToken: String
        @JvmSynthetic @JvmName("ktGetAccountToken") inline get() = getAccountToken()

    open fun getPushTypes(): ArrayList<PushType> = _pushTypes

    /** Kotlin property syntax, equivalent to [getPushTypes]. */
    val pushTypes: ArrayList<PushType>
        @JvmSynthetic @JvmName("ktGetPushTypes") inline get() = getPushTypes()

    open fun addPushType(allowedPushType: PushType) {
        if (!_pushTypes.contains(allowedPushType)) {
            this._pushTypes.add(allowedPushType)
        }
    }

    /**
     * Debug level of this instance. Setting it also updates this instance's [logger].
     */
    open fun getDebugLevel(): Int = _debugLevel

    open fun setDebugLevel(debugLevel: Int) {
        this._debugLevel = debugLevel
        val currentLogger = _logger
        if (currentLogger != null) {
            currentLogger.setDebugLevel(debugLevel)
        }
    }

    /** Kotlin property syntax, equivalent to [getDebugLevel] / [setDebugLevel]. */
    var debugLevel: Int
        @JvmSynthetic @JvmName("ktGetDebugLevel") inline get() = getDebugLevel()
        @JvmSynthetic @JvmName("ktSetDebugLevel") inline set(value) = setDebugLevel(value)

    open fun getProxyDomain(): String? = _proxyDomain

    open fun setProxyDomain(proxyDomain: String?) {
        this._proxyDomain = proxyDomain
    }

    /** Kotlin property syntax, equivalent to [getProxyDomain] / [setProxyDomain]. */
    var proxyDomain: String?
        @JvmSynthetic @JvmName("ktGetProxyDomain") inline get() = getProxyDomain()
        @JvmSynthetic @JvmName("ktSetProxyDomain") inline set(value) = setProxyDomain(value)

    open fun getSpikyProxyDomain(): String? = _spikyProxyDomain

    open fun setSpikyProxyDomain(spikyProxyDomain: String?) {
        this._spikyProxyDomain = spikyProxyDomain
    }

    /** Kotlin property syntax, equivalent to [getSpikyProxyDomain] / [setSpikyProxyDomain]. */
    var spikyProxyDomain: String?
        @JvmSynthetic @JvmName("ktGetSpikyProxyDomain") inline get() = getSpikyProxyDomain()
        @JvmSynthetic @JvmName("ktSetSpikyProxyDomain") inline set(value) = setSpikyProxyDomain(value)

    open fun getCustomHandshakeDomain(): String? = _customHandshakeDomain

    open fun setCustomHandshakeDomain(handshakeDomain: String?) {
        this._customHandshakeDomain = handshakeDomain
    }

    /** Kotlin property syntax, equivalent to [getCustomHandshakeDomain] / [setCustomHandshakeDomain]. */
    var customHandshakeDomain: String?
        @JvmSynthetic @JvmName("ktGetCustomHandshakeDomain") inline get() = getCustomHandshakeDomain()
        @JvmSynthetic @JvmName("ktSetCustomHandshakeDomain") inline set(value) = setCustomHandshakeDomain(value)

    open fun setDebugLevel(debugLevel: CleverTapAPI.LogLevel) {
        this.debugLevel = debugLevel.intValue()
    }

    open fun getFcmSenderId(): String? = _fcmSenderId

    /** Kotlin property syntax, equivalent to [getFcmSenderId]. */
    val fcmSenderId: String?
        @JvmSynthetic @JvmName("ktGetFcmSenderId") inline get() = getFcmSenderId()

    open fun getLogger(): Logger {
        var currentLogger = _logger
        if (currentLogger == null) {
            currentLogger = Logger(this._debugLevel)
            _logger = currentLogger
        }
        return currentLogger
    }

    /** Kotlin property syntax, equivalent to [getLogger]. */
    val logger: Logger
        @JvmSynthetic @JvmName("ktGetLogger") inline get() = getLogger()

    open fun getPackageName(): String? = _packageName

    /** Kotlin property syntax, equivalent to [getPackageName]. */
    val packageName: String?
        @JvmSynthetic @JvmName("ktGetPackageName") inline get() = getPackageName()

    /**
     * Identity keys used for user login. Set them with [setIdentityKeys].
     */
    open fun getIdentityKeys(): Array<String>? = _identityKeys

    /** Kotlin property syntax, equivalent to [getIdentityKeys]. */
    val identityKeys: Array<String>?
        @JvmSynthetic @JvmName("ktGetIdentityKeys") inline get() = getIdentityKeys()

    open fun isAnalyticsOnly(): Boolean = _analyticsOnly

    open fun setAnalyticsOnly(analyticsOnly: Boolean) {
        this._analyticsOnly = analyticsOnly
    }

    /** Kotlin property syntax, equivalent to [isAnalyticsOnly] / [setAnalyticsOnly]. */
    var isAnalyticsOnly: Boolean
        @JvmSynthetic @JvmName("ktIsAnalyticsOnly") inline get() = isAnalyticsOnly()
        @JvmSynthetic @JvmName("ktSetAnalyticsOnly") inline set(value) = setAnalyticsOnly(value)

    open fun isBeta(): Boolean = _beta

    /** Kotlin property syntax, equivalent to [isBeta]. */
    val isBeta: Boolean
        @JvmSynthetic @JvmName("ktIsBeta") inline get() = isBeta()

    open fun isDefaultInstance(): Boolean = _isDefaultInstance

    /** Kotlin property syntax, equivalent to [isDefaultInstance]. */
    val isDefaultInstance: Boolean
        @JvmSynthetic @JvmName("ktIsDefaultInstance") inline get() = isDefaultInstance()

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    open fun log(tag: String, message: String) {
        logger.verbose(getDefaultSuffix(tag), message)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    open fun log(tag: String, message: String, throwable: Throwable?) {
        logger.verbose(getDefaultSuffix(tag), message, throwable)
    }

    open fun setIdentityKeys(@IdentityType vararg identityKeys: String) {
        if (!_isDefaultInstance) {
            @Suppress("UNCHECKED_CAST")
            this._identityKeys = identityKeys as Array<String>
            log(
                LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys via setter: " + Arrays
                    .toString(this._identityKeys)
            )
        }
    }

    open fun useGoogleAdId(value: Boolean) {
        this._useGoogleAdId = value
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(_accountId)
        dest.writeString(_accountToken)
        dest.writeString(_accountRegion)
        dest.writeString(_proxyDomain)
        dest.writeString(_spikyProxyDomain)
        dest.writeString(_customHandshakeDomain)
        dest.writeByte((if (_analyticsOnly) 0x01 else 0x00).toByte())
        dest.writeByte((if (_isDefaultInstance) 0x01 else 0x00).toByte())
        dest.writeByte((if (_useGoogleAdId) 0x01 else 0x00).toByte())
        dest.writeByte((if (_disableAppLaunchedEvent) 0x01 else 0x00).toByte())
        dest.writeByte((if (_personalization) 0x01 else 0x00).toByte())
        dest.writeInt(_debugLevel)
        dest.writeByte((if (_createdPostAppLaunch) 0x01 else 0x00).toByte())
        dest.writeByte((if (_sslPinning) 0x01 else 0x00).toByte())
        dest.writeByte((if (_backgroundSync) 0x01 else 0x00).toByte())
        dest.writeByte((if (_enableCustomCleverTapId) 0x01 else 0x00).toByte())
        dest.writeString(_fcmSenderId)
        dest.writeString(_packageName)
        dest.writeByte((if (_beta) 0x01 else 0x00).toByte())
        dest.writeStringArray(_identityKeys)
        dest.writeInt(_encryptionLevel)
        dest.writeString(_encryptionInTransit)
        val allowTypesString = getPushTypesArray().toString()
        dest.writeString(allowTypesString)
    }

    open fun getEnableCustomCleverTapId(): Boolean = _enableCustomCleverTapId

    open fun setEnableCustomCleverTapId(enableCustomCleverTapId: Boolean) {
        this._enableCustomCleverTapId = enableCustomCleverTapId
    }

    /** Kotlin property syntax, equivalent to [getEnableCustomCleverTapId] / [setEnableCustomCleverTapId]. */
    var enableCustomCleverTapId: Boolean
        @JvmSynthetic @JvmName("ktGetEnableCustomCleverTapId") inline get() = getEnableCustomCleverTapId()
        @JvmSynthetic @JvmName("ktSetEnableCustomCleverTapId") inline set(value) = setEnableCustomCleverTapId(value)

    /**
     * Background sync flag. The getter is SDK-internal (`@RestrictTo(LIBRARY)`), the setter is public API.
     */
    @RestrictTo(Scope.LIBRARY)
    open fun isBackgroundSync(): Boolean = _backgroundSync

    open fun setBackgroundSync(backgroundSync: Boolean) {
        this._backgroundSync = backgroundSync
    }

    /** Kotlin property syntax, equivalent to [isBackgroundSync] / [setBackgroundSync]. */
    var isBackgroundSync: Boolean
        @JvmSynthetic @JvmName("ktIsBackgroundSync") inline get() = isBackgroundSync()
        @JvmSynthetic @JvmName("ktSetBackgroundSync") inline set(value) = setBackgroundSync(value)

    open fun isCreatedPostAppLaunch(): Boolean = _createdPostAppLaunch

    /** Kotlin property syntax, equivalent to [isCreatedPostAppLaunch]. */
    val isCreatedPostAppLaunch: Boolean
        @JvmSynthetic @JvmName("ktIsCreatedPostAppLaunch") inline get() = isCreatedPostAppLaunch()

    /**
     * Whether the App Launched event is disabled. The getter was package-private in Java and is
     * SDK-internal (`@RestrictTo(LIBRARY)`); the setter is public API.
     */
    @RestrictTo(Scope.LIBRARY)
    open fun isDisableAppLaunchedEvent(): Boolean = _disableAppLaunchedEvent

    open fun setDisableAppLaunchedEvent(disableAppLaunchedEvent: Boolean) {
        this._disableAppLaunchedEvent = disableAppLaunchedEvent
    }

    /** Kotlin property syntax, equivalent to [isDisableAppLaunchedEvent] / [setDisableAppLaunchedEvent]. */
    var isDisableAppLaunchedEvent: Boolean
        @JvmSynthetic @JvmName("ktIsDisableAppLaunchedEvent") inline get() = isDisableAppLaunchedEvent()
        @JvmSynthetic @JvmName("ktSetDisableAppLaunchedEvent") inline set(value) = setDisableAppLaunchedEvent(value)

    @get:JvmName("isPersonalizationEnabled")
    internal val isPersonalizationEnabled: Boolean
        get() = _personalization

    open fun isSslPinningEnabled(): Boolean = _sslPinning

    /** Kotlin property syntax, equivalent to [isSslPinningEnabled]. */
    val isSslPinningEnabled: Boolean
        @JvmSynthetic @JvmName("ktIsSslPinningEnabled") inline get() = isSslPinningEnabled()

    @get:JvmName("isUseGoogleAdId")
    internal val isUseGoogleAdId: Boolean
        get() = _useGoogleAdId

    @JvmName("setCreatedPostAppLaunch")
    internal fun setCreatedPostAppLaunch() {
        this._createdPostAppLaunch = true
    }

    open fun setEncryptionLevel(encryptionLevel: EncryptionLevel) {
        this._encryptionLevel = encryptionLevel.intValue()
    }

    open fun getEncryptionLevel(): Int = _encryptionLevel

    /** Kotlin property syntax, equivalent to [getEncryptionLevel]. */
    val encryptionLevel: Int
        @JvmSynthetic @JvmName("ktGetEncryptionLevel") inline get() = getEncryptionLevel()

    open fun setEncryptionInTransit(encryptionInTransit: Boolean) {
        this._encryptionInTransit = if (encryptionInTransit) "1" else "0"
    }

    open fun isEncryptionInTransitEnabled(): Boolean {
        try {
            return Integer.parseInt(_encryptionInTransit) > 0
        } catch (e: NumberFormatException) {
            Logger.v("Invalid value passed in manifest for encryption in transit")
            return false
        }
    }

    /** Kotlin property syntax, equivalent to [isEncryptionInTransitEnabled]. */
    val isEncryptionInTransitEnabled: Boolean
        @JvmSynthetic @JvmName("ktIsEncryptionInTransitEnabled") inline get() = isEncryptionInTransitEnabled()

    @JvmName("toJSONString")
    internal fun toJSONString(): String? {
        val configJsonObject = JSONObject()
        try {
            configJsonObject.put(KEY_ACCOUNT_ID, _accountId)
            configJsonObject.put(KEY_ACCOUNT_TOKEN, _accountToken)
            configJsonObject.put(KEY_ACCOUNT_REGION, accountRegion)
            configJsonObject.put(KEY_PROXY_DOMAIN, proxyDomain)
            configJsonObject.put(KEY_SPIKY_PROXY_DOMAIN, spikyProxyDomain)
            configJsonObject.put(KEY_CUSTOM_HANDSHAKE_DOMAIN, customHandshakeDomain)
            configJsonObject.put(KEY_FCM_SENDER_ID, fcmSenderId)
            configJsonObject.put(KEY_ANALYTICS_ONLY, isAnalyticsOnly)
            configJsonObject.put(KEY_DEFAULT_INSTANCE, isDefaultInstance)
            configJsonObject.put(KEY_USE_GOOGLE_AD_ID, isUseGoogleAdId)
            configJsonObject.put(KEY_DISABLE_APP_LAUNCHED, isDisableAppLaunchedEvent)
            configJsonObject.put(KEY_PERSONALIZATION, isPersonalizationEnabled)
            configJsonObject.put(KEY_DEBUG_LEVEL, debugLevel)
            configJsonObject.put(KEY_CREATED_POST_APP_LAUNCH, isCreatedPostAppLaunch)
            configJsonObject.put(KEY_SSL_PINNING, isSslPinningEnabled)
            configJsonObject.put(KEY_BACKGROUND_SYNC, isBackgroundSync)
            configJsonObject.put(KEY_ENABLE_CUSTOM_CT_ID, enableCustomCleverTapId)
            configJsonObject.put(KEY_PACKAGE_NAME, packageName)
            configJsonObject.put(KEY_BETA, isBeta)
            configJsonObject.put(KEY_ENCRYPTION_LEVEL, encryptionLevel)
            configJsonObject.put(KEY_ENCRYPTION_IN_TRANSIT, _encryptionInTransit)
            val pushTypesArray = getPushTypesArray()
            configJsonObject.put(KEY_PUSH_TYPES, pushTypesArray)

            return configJsonObject.toString()
        } catch (e: Throwable) {
            Logger.v("Unable to convert config to JSON : ", e.cause)
            return null
        }
    }

    private fun getPushTypesArray(): JSONArray {
        val pushTypesArray = JSONArray()
        for (pushType in pushTypes) {
            // fcm is always loaded by default.
            if (pushType !== PushConstants.FCM) {
                pushTypesArray.put(pushType.toJSONObject())
            }
        }
        return pushTypesArray
    }

    private fun getDefaultSuffix(tag: String): String {
        return "[" + ((if (!TextUtils.isEmpty(tag)) ":$tag" else "") + ":" + _accountId + "]")
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CleverTapInstanceConfig> =
            object : Parcelable.Creator<CleverTapInstanceConfig> {
                override fun createFromParcel(`in`: Parcel): CleverTapInstanceConfig {
                    return CleverTapInstanceConfig(`in`)
                }

                override fun newArray(size: Int): Array<CleverTapInstanceConfig?> {
                    return arrayOfNulls(size)
                }
            }

        //Keys used by the SDK
        private const val KEY_ACCOUNT_ID = "accountId"
        private const val KEY_ACCOUNT_TOKEN = "accountToken"
        private const val KEY_ACCOUNT_REGION = "accountRegion"
        private const val KEY_PROXY_DOMAIN = "proxyDomain"
        private const val KEY_SPIKY_PROXY_DOMAIN = "spikyProxyDomain"
        private const val KEY_CUSTOM_HANDSHAKE_DOMAIN = "customHandshakeDomain"
        private const val KEY_FCM_SENDER_ID = "fcmSenderId"
        private const val KEY_ANALYTICS_ONLY = "analyticsOnly"
        private const val KEY_DEFAULT_INSTANCE = "isDefaultInstance"
        private const val KEY_USE_GOOGLE_AD_ID = "useGoogleAdId"
        private const val KEY_DISABLE_APP_LAUNCHED = "disableAppLaunchedEvent"
        private const val KEY_PERSONALIZATION = "personalization"
        private const val KEY_DEBUG_LEVEL = "debugLevel"
        private const val KEY_CREATED_POST_APP_LAUNCH = "createdPostAppLaunch"
        private const val KEY_SSL_PINNING = "sslPinning"
        private const val KEY_BACKGROUND_SYNC = "backgroundSync"
        private const val KEY_ENABLE_CUSTOM_CT_ID = "getEnableCustomCleverTapId"
        private const val KEY_BETA = "beta"
        private const val KEY_IDENTITY_TYPES = "identityTypes"
        private const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_ENCRYPTION_LEVEL = "encryptionLevel"
        private const val KEY_PUSH_TYPES = "allowedPushTypes"
        private const val KEY_ENCRYPTION_IN_TRANSIT = "encryptionInTransit"

        /**
         * Creates a CleverTapInstanceConfig with meta data from manifest file
         * @param context Application context
         * @return CleverTapInstanceConfig, or `null` if the account ID or token is missing from the manifest
         */
        @JvmStatic
        fun getDefaultInstance(context: Context?): CleverTapInstanceConfig? {
            val info = ManifestInfo.getInstance(context)
            val accountId = info.accountId
            val accountToken = info.accountToken
            if (accountId == null || accountToken == null) {
                Logger.i("Account ID or Account token is missing from AndroidManifest.xml, unable to create default CleverTapInstanceConfig")
                return null
            }
            return createInstanceWithManifest(
                info,
                accountId,
                accountToken,
                info.getAccountRegion(),
                true
            )
        }

        /**
         * Creates a CleverTapInstanceConfig for an additional instance.
         *
         * @return the config, or `null` if [accountId] or [accountToken] is null
         */
        @JvmStatic
        fun createInstance(
            context: Context?,
            accountId: String?,
            accountToken: String?
        ): CleverTapInstanceConfig? {
            return createInstance(context, accountId, accountToken, null)
        }

        /**
         * Creates a CleverTapInstanceConfig for an additional instance.
         *
         * @return the config, or `null` if [accountId] or [accountToken] is null
         */
        @JvmStatic
        fun createInstance(
            context: Context?,
            accountId: String?,
            accountToken: String?,
            accountRegion: String?
        ): CleverTapInstanceConfig? {
            if (accountId == null || accountToken == null) {
                Logger.i("CleverTap accountId and accountToken cannot be null")
                return null
            }
            val manifestInfo = ManifestInfo.getInstance(context)
            return createInstanceWithManifest(manifestInfo, accountId, accountToken, accountRegion, false)
        }

        // convenience to construct the internal only default config
        @JvmStatic
        protected fun createDefaultInstance(
            context: Context,
            accountId: String,
            accountToken: String,
            accountRegion: String?
        ): CleverTapInstanceConfig {
            val manifestInfo = ManifestInfo.getInstance(context)
            return createInstanceWithManifest(manifestInfo, accountId, accountToken, accountRegion, true)
        }

        // @RestrictTo keeps the BCV dump clean: @JvmName makes BCV treat this internal member as public.
        @RestrictTo(Scope.LIBRARY)
        @JvmStatic
        @JvmName("createInstanceWithManifest")
        internal fun createInstanceWithManifest(
            manifest: ManifestInfo,
            accountId: String?,
            accountToken: String?,
            accountRegion: String?,
            isDefaultInstance: Boolean
        ): CleverTapInstanceConfig {
            return CleverTapInstanceConfig(manifest, accountId, accountToken, accountRegion, isDefaultInstance)
        }

        // for internal use only!
        @JvmStatic
        protected fun createInstance(jsonString: String?): CleverTapInstanceConfig? {
            return createInstanceFromJson(jsonString)
        }

        /**
         * SDK-internal access to [createInstance] (String): Kotlin `protected` companion members are
         * not visible to other classes in the package, unlike Java `protected static`.
         */
        internal fun createInstanceFromJson(jsonString: String?): CleverTapInstanceConfig? {
            try {
                return CleverTapInstanceConfig(jsonString)
            } catch (t: Throwable) {
                return null
            }
        }
    }
}
