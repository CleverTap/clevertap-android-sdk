package com.clevertap.android.sdk

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

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

public open class CleverTapInstanceConfig : Parcelable {

    private var accountIdValue: String? = null
    private var accountRegionValue: String? = null
    private var accountTokenValue: String? = null
    private var proxyDomainValue: String? = null
    private var spikyProxyDomainValue: String? = null
    private var customHandshakeDomainValue: String? = null
    private val pushTypesValue: ArrayList<PushType> = getDefaultPushTypes()
    private var analyticsOnlyValue = false
    private var backgroundSyncValue = false
    private var beta = false
    private var createdPostAppLaunch = false
    private var debugLevelValue = 0
    private var disableAppLaunchedEventValue = false
    private var enableCustomCleverTapIdValue = false
    private var fcmSenderIdValue: String? = null
    private var defaultInstance = false
    private lateinit var loggerValue: Logger
    private var packageNameValue: String? = null
    private var personalization = false
    private var identityKeysValue: Array<String>? = Constants.NULL_STRING_ARRAY
    private var sslPinning = false
    private var useGoogleAdId = false
    private var encryptionLevelValue = 0
    private var encryptionInTransitValue: String? = null

    internal constructor(config: CleverTapInstanceConfig) {
        accountIdValue = config.accountIdValue
        accountTokenValue = config.accountTokenValue
        accountRegionValue = config.accountRegionValue
        proxyDomainValue = config.proxyDomainValue
        spikyProxyDomainValue = config.spikyProxyDomainValue
        customHandshakeDomainValue = config.customHandshakeDomainValue
        defaultInstance = config.defaultInstance
        analyticsOnlyValue = config.analyticsOnlyValue
        personalization = config.personalization
        debugLevelValue = config.debugLevelValue
        loggerValue = config.loggerValue
        useGoogleAdId = config.useGoogleAdId
        disableAppLaunchedEventValue = config.disableAppLaunchedEventValue
        createdPostAppLaunch = config.createdPostAppLaunch
        sslPinning = config.sslPinning
        backgroundSyncValue = config.backgroundSyncValue
        enableCustomCleverTapIdValue = config.enableCustomCleverTapIdValue
        fcmSenderIdValue = config.fcmSenderIdValue
        packageNameValue = config.packageNameValue
        beta = config.beta
        identityKeysValue = config.identityKeysValue
        encryptionLevelValue = config.encryptionLevelValue
        config.pushTypesValue.forEach(::addPushType)
        encryptionInTransitValue = config.encryptionInTransitValue
    }

    private constructor(
        manifest: ManifestInfo,
        accountId: String?,
        accountToken: String?,
        accountRegion: String?,
        isDefault: Boolean
    ) {
        accountIdValue = accountId
        accountTokenValue = accountToken
        accountRegionValue = accountRegion
        defaultInstance = isDefault
        analyticsOnlyValue = false
        personalization = true
        debugLevelValue = CleverTapAPI.LogLevel.INFO.intValue()
        loggerValue = Logger(debugLevelValue)
        createdPostAppLaunch = false

        useGoogleAdId = manifest.useGoogleAdId()
        disableAppLaunchedEventValue = manifest.isAppLaunchedDisabled
        sslPinning = manifest.isSSLPinningEnabled
        backgroundSyncValue = manifest.isBackgroundSync
        fcmSenderIdValue = manifest.fcmSenderId
        packageNameValue = manifest.packageName
        enableCustomCleverTapIdValue = manifest.useCustomId()
        beta = manifest.enableBeta()
        /*
         * For default instance, use manifest meta, otherwise use from setter field
         */
        if (defaultInstance) {
            encryptionLevelValue = manifest.encryptionLevel
            identityKeysValue = manifest.profileKeys
            log(
                LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "Setting Profile Keys from Manifest: ${identityKeysValue.contentToString()}"
            )
        } else {
            encryptionLevelValue = 0
        }
        buildPushProvidersFromManifest(manifest)

        encryptionInTransitValue = manifest.encryptionInTransit ?: "0"
    }

    private fun buildPushProvidersFromManifest(manifest: ManifestInfo) {
        try {
            addPushTypeFromManifest(manifest.vendorOneProvider)
            addPushTypeFromManifest(manifest.vendorTwoProvider)
        } catch (e: Exception) {
            Logger.v("There was some problem in loading push providers from manifest")
        }
    }

    /** Parses a `type,prefKey,className,messagingSDKClassName` manifest entry into a [PushType]. */
    private fun addPushTypeFromManifest(provider: String?) {
        val splits = provider?.split(",")?.dropLastWhile { it.isEmpty() } ?: return
        if (splits.size == 4) {
            addPushType(PushType(splits[0].trim(), splits[1].trim(), splits[2].trim(), splits[3].trim()))
        }
    }

    private constructor(jsonString: String?) {
        try {
            val json = JSONObject(jsonString)
            if (json.has(KEY_ACCOUNT_ID)) {
                accountIdValue = json.getString(KEY_ACCOUNT_ID)
            }
            if (json.has(KEY_ACCOUNT_TOKEN)) {
                accountTokenValue = json.getString(KEY_ACCOUNT_TOKEN)
            }
            if (json.has(KEY_PROXY_DOMAIN)) {
                proxyDomainValue = json.getString(KEY_PROXY_DOMAIN)
            }
            if (json.has(KEY_SPIKY_PROXY_DOMAIN)) {
                spikyProxyDomainValue = json.getString(KEY_SPIKY_PROXY_DOMAIN)
            }
            if (json.has(KEY_CUSTOM_HANDSHAKE_DOMAIN)) {
                customHandshakeDomainValue = json.optString(KEY_CUSTOM_HANDSHAKE_DOMAIN, null)
            }
            if (json.has(KEY_ACCOUNT_REGION)) {
                accountRegionValue = json.getString(KEY_ACCOUNT_REGION)
            }
            if (json.has(KEY_ANALYTICS_ONLY)) {
                analyticsOnlyValue = json.getBoolean(KEY_ANALYTICS_ONLY)
            }
            if (json.has(KEY_DEFAULT_INSTANCE)) {
                defaultInstance = json.getBoolean(KEY_DEFAULT_INSTANCE)
            }
            if (json.has(KEY_USE_GOOGLE_AD_ID)) {
                useGoogleAdId = json.getBoolean(KEY_USE_GOOGLE_AD_ID)
            }
            if (json.has(KEY_DISABLE_APP_LAUNCHED)) {
                disableAppLaunchedEventValue = json.getBoolean(KEY_DISABLE_APP_LAUNCHED)
            }
            if (json.has(KEY_PERSONALIZATION)) {
                personalization = json.getBoolean(KEY_PERSONALIZATION)
            }
            if (json.has(KEY_DEBUG_LEVEL)) {
                debugLevelValue = json.getInt(KEY_DEBUG_LEVEL)
            }
            loggerValue = Logger(debugLevelValue)

            if (json.has(KEY_PACKAGE_NAME)) {
                packageNameValue = json.getString(KEY_PACKAGE_NAME)
            }
            if (json.has(KEY_CREATED_POST_APP_LAUNCH)) {
                createdPostAppLaunch = json.getBoolean(KEY_CREATED_POST_APP_LAUNCH)
            }
            if (json.has(KEY_SSL_PINNING)) {
                sslPinning = json.getBoolean(KEY_SSL_PINNING)
            }
            if (json.has(KEY_BACKGROUND_SYNC)) {
                backgroundSyncValue = json.getBoolean(KEY_BACKGROUND_SYNC)
            }
            if (json.has(KEY_ENABLE_CUSTOM_CT_ID)) {
                enableCustomCleverTapIdValue = json.getBoolean(KEY_ENABLE_CUSTOM_CT_ID)
            }
            if (json.has(KEY_FCM_SENDER_ID)) {
                fcmSenderIdValue = json.getString(KEY_FCM_SENDER_ID)
            }
            if (json.has(KEY_BETA)) {
                beta = json.getBoolean(KEY_BETA)
            }
            if (json.has(KEY_IDENTITY_TYPES)) {
                @Suppress("UNCHECKED_CAST")
                identityKeysValue = toArray<Any>(json.getJSONArray(KEY_IDENTITY_TYPES)) as Array<String>
            }
            if (json.has(KEY_ENCRYPTION_LEVEL)) {
                encryptionLevelValue = json.getInt(KEY_ENCRYPTION_LEVEL)
            }
            if (json.has(KEY_PUSH_TYPES)) {
                val pushTypesArray = json.getJSONArray(KEY_PUSH_TYPES)
                for (i in 0 until pushTypesArray.length()) {
                    PushType.fromJSONObject(pushTypesArray.getJSONObject(i))?.let(::addPushType)
                }
            }
            encryptionInTransitValue = json.optString(KEY_ENCRYPTION_IN_TRANSIT, "0")
        } catch (t: Throwable) {
            Logger.v("Error constructing CleverTapInstanceConfig from JSON: $jsonString: ", t.cause)
            throw t
        }
    }

    private constructor(parcel: Parcel) {
        accountIdValue = parcel.readString()
        accountTokenValue = parcel.readString()
        accountRegionValue = parcel.readString()
        proxyDomainValue = parcel.readString()
        spikyProxyDomainValue = parcel.readString()
        customHandshakeDomainValue = parcel.readString()
        analyticsOnlyValue = parcel.readFlag()
        defaultInstance = parcel.readFlag()
        useGoogleAdId = parcel.readFlag()
        disableAppLaunchedEventValue = parcel.readFlag()
        personalization = parcel.readFlag()
        debugLevelValue = parcel.readInt()
        createdPostAppLaunch = parcel.readFlag()
        sslPinning = parcel.readFlag()
        backgroundSyncValue = parcel.readFlag()
        enableCustomCleverTapIdValue = parcel.readFlag()
        fcmSenderIdValue = parcel.readString()
        packageNameValue = parcel.readString()
        loggerValue = Logger(debugLevelValue)
        beta = parcel.readFlag()
        identityKeysValue = parcel.createStringArray()
        encryptionLevelValue = parcel.readInt()
        encryptionInTransitValue = parcel.readString()
        try {
            val allowedTypesJsonArray = JSONArray(parcel.readString())
            for (i in 0 until allowedTypesJsonArray.length()) {
                PushType.fromJSONObject(allowedTypesJsonArray.getJSONObject(i))?.let(::addPushType)
            }
        } catch (e: JSONException) {
            Logger.v("Error in loading push providers from parcel, using firebase")
        }
    }

    override fun describeContents(): Int = 0

    public open fun enablePersonalization(enablePersonalization: Boolean) {
        personalization = enablePersonalization
    }

    /**
     * The CleverTap account ID. Every public factory rejects a null account ID, so this is non-null
     * for any config created through the public API.
     */
    public open fun getAccountId(): String =
        accountIdValue ?: throw NullPointerException("CleverTapInstanceConfig has no account ID")

    /** Kotlin property syntax, equivalent to [getAccountId]. */
    public val accountId: String
        @JvmSynthetic @JvmName("ktGetAccountId") inline get() = getAccountId()

    public open fun getAccountRegion(): String? = accountRegionValue

    /** Kotlin property syntax, equivalent to [getAccountRegion]. */
    public val accountRegion: String?
        @JvmSynthetic @JvmName("ktGetAccountRegion") inline get() = getAccountRegion()

    /**
     * The CleverTap account token. Every public factory rejects a null token, so this is non-null
     * for any config created through the public API.
     */
    public open fun getAccountToken(): String =
        accountTokenValue ?: throw NullPointerException("CleverTapInstanceConfig has no account token")

    /** Kotlin property syntax, equivalent to [getAccountToken]. */
    public val accountToken: String
        @JvmSynthetic @JvmName("ktGetAccountToken") inline get() = getAccountToken()

    public open fun getPushTypes(): ArrayList<PushType> = pushTypesValue

    /** Kotlin property syntax, equivalent to [getPushTypes]. */
    public val pushTypes: ArrayList<PushType>
        @JvmSynthetic @JvmName("ktGetPushTypes") inline get() = getPushTypes()

    public open fun addPushType(allowedPushType: PushType) {
        if (allowedPushType !in pushTypesValue) {
            pushTypesValue.add(allowedPushType)
        }
    }

    /**
     * Debug level of this instance. Setting it also updates this instance's [logger].
     */
    public open fun getDebugLevel(): Int = debugLevelValue

    public open fun setDebugLevel(debugLevel: Int) {
        debugLevelValue = debugLevel
        loggerValue.setDebugLevel(debugLevel)
    }

    /** Kotlin property syntax, equivalent to [getDebugLevel] / [setDebugLevel]. */
    public var debugLevel: Int
        @JvmSynthetic @JvmName("ktGetDebugLevel") inline get() = getDebugLevel()
        @JvmSynthetic @JvmName("ktSetDebugLevel") inline set(value) = setDebugLevel(value)

    public open fun getProxyDomain(): String? = proxyDomainValue

    public open fun setProxyDomain(proxyDomain: String?) {
        proxyDomainValue = proxyDomain
    }

    /** Kotlin property syntax, equivalent to [getProxyDomain] / [setProxyDomain]. */
    public var proxyDomain: String?
        @JvmSynthetic @JvmName("ktGetProxyDomain") inline get() = getProxyDomain()
        @JvmSynthetic @JvmName("ktSetProxyDomain") inline set(value) = setProxyDomain(value)

    public open fun getSpikyProxyDomain(): String? = spikyProxyDomainValue

    public open fun setSpikyProxyDomain(spikyProxyDomain: String?) {
        spikyProxyDomainValue = spikyProxyDomain
    }

    /** Kotlin property syntax, equivalent to [getSpikyProxyDomain] / [setSpikyProxyDomain]. */
    public var spikyProxyDomain: String?
        @JvmSynthetic @JvmName("ktGetSpikyProxyDomain") inline get() = getSpikyProxyDomain()
        @JvmSynthetic @JvmName("ktSetSpikyProxyDomain") inline set(value) = setSpikyProxyDomain(value)

    public open fun getCustomHandshakeDomain(): String? = customHandshakeDomainValue

    public open fun setCustomHandshakeDomain(handshakeDomain: String?) {
        customHandshakeDomainValue = handshakeDomain
    }

    /** Kotlin property syntax, equivalent to [getCustomHandshakeDomain] / [setCustomHandshakeDomain]. */
    public var customHandshakeDomain: String?
        @JvmSynthetic @JvmName("ktGetCustomHandshakeDomain") inline get() = getCustomHandshakeDomain()
        @JvmSynthetic @JvmName("ktSetCustomHandshakeDomain") inline set(value) = setCustomHandshakeDomain(value)

    public open fun setDebugLevel(debugLevel: CleverTapAPI.LogLevel) {
        setDebugLevel(debugLevel.intValue())
    }

    public open fun getFcmSenderId(): String? = fcmSenderIdValue

    /** Kotlin property syntax, equivalent to [getFcmSenderId]. */
    public val fcmSenderId: String?
        @JvmSynthetic @JvmName("ktGetFcmSenderId") inline get() = getFcmSenderId()

    public open fun getLogger(): Logger = loggerValue

    /** Kotlin property syntax, equivalent to [getLogger]. */
    public val logger: Logger
        @JvmSynthetic @JvmName("ktGetLogger") inline get() = getLogger()

    public open fun getPackageName(): String? = packageNameValue

    /** Kotlin property syntax, equivalent to [getPackageName]. */
    public val packageName: String?
        @JvmSynthetic @JvmName("ktGetPackageName") inline get() = getPackageName()

    /**
     * Identity keys used for user login. Set them with [setIdentityKeys].
     */
    public open fun getIdentityKeys(): Array<String>? = identityKeysValue

    /** Kotlin property syntax, equivalent to [getIdentityKeys]. */
    public val identityKeys: Array<String>?
        @JvmSynthetic @JvmName("ktGetIdentityKeys") inline get() = getIdentityKeys()

    public open fun isAnalyticsOnly(): Boolean = analyticsOnlyValue

    public open fun setAnalyticsOnly(analyticsOnly: Boolean) {
        analyticsOnlyValue = analyticsOnly
    }

    /** Kotlin property syntax, equivalent to [isAnalyticsOnly] / [setAnalyticsOnly]. */
    public var isAnalyticsOnly: Boolean
        @JvmSynthetic @JvmName("ktIsAnalyticsOnly") inline get() = isAnalyticsOnly()
        @JvmSynthetic @JvmName("ktSetAnalyticsOnly") inline set(value) = setAnalyticsOnly(value)

    public open fun isBeta(): Boolean = beta

    /** Kotlin property syntax, equivalent to [isBeta]. */
    public val isBeta: Boolean
        @JvmSynthetic @JvmName("ktIsBeta") inline get() = isBeta()

    public open fun isDefaultInstance(): Boolean = defaultInstance

    /** Kotlin property syntax, equivalent to [isDefaultInstance]. */
    public val isDefaultInstance: Boolean
        @JvmSynthetic @JvmName("ktIsDefaultInstance") inline get() = isDefaultInstance()

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public open fun log(tag: String, message: String) {
        logger.verbose(defaultSuffix(tag), message)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public open fun log(tag: String, message: String, throwable: Throwable?) {
        logger.verbose(defaultSuffix(tag), message, throwable)
    }

    public open fun setIdentityKeys(@IdentityType vararg identityKeys: String) {
        if (!defaultInstance) {
            @Suppress("UNCHECKED_CAST")
            identityKeysValue = identityKeys as Array<String>
            log(
                LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "Setting Profile Keys via setter: ${identityKeysValue.contentToString()}"
            )
        }
    }

    public open fun useGoogleAdId(value: Boolean) {
        useGoogleAdId = value
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(accountIdValue)
        dest.writeString(accountTokenValue)
        dest.writeString(accountRegionValue)
        dest.writeString(proxyDomainValue)
        dest.writeString(spikyProxyDomainValue)
        dest.writeString(customHandshakeDomainValue)
        dest.writeFlag(analyticsOnlyValue)
        dest.writeFlag(defaultInstance)
        dest.writeFlag(useGoogleAdId)
        dest.writeFlag(disableAppLaunchedEventValue)
        dest.writeFlag(personalization)
        dest.writeInt(debugLevelValue)
        dest.writeFlag(createdPostAppLaunch)
        dest.writeFlag(sslPinning)
        dest.writeFlag(backgroundSyncValue)
        dest.writeFlag(enableCustomCleverTapIdValue)
        dest.writeString(fcmSenderIdValue)
        dest.writeString(packageNameValue)
        dest.writeFlag(beta)
        dest.writeStringArray(identityKeysValue)
        dest.writeInt(encryptionLevelValue)
        dest.writeString(encryptionInTransitValue)
        dest.writeString(pushTypesJson().toString())
    }

    public open fun getEnableCustomCleverTapId(): Boolean = enableCustomCleverTapIdValue

    public open fun setEnableCustomCleverTapId(enableCustomCleverTapId: Boolean) {
        enableCustomCleverTapIdValue = enableCustomCleverTapId
    }

    /** Kotlin property syntax, equivalent to [getEnableCustomCleverTapId] / [setEnableCustomCleverTapId]. */
    public var enableCustomCleverTapId: Boolean
        @JvmSynthetic @JvmName("ktGetEnableCustomCleverTapId") inline get() = getEnableCustomCleverTapId()
        @JvmSynthetic @JvmName("ktSetEnableCustomCleverTapId") inline set(value) = setEnableCustomCleverTapId(value)

    /**
     * Background sync flag. The getter is SDK-internal (`@RestrictTo(LIBRARY)`), the setter is public API.
     */
    @RestrictTo(Scope.LIBRARY)
    public open fun isBackgroundSync(): Boolean = backgroundSyncValue

    public open fun setBackgroundSync(backgroundSync: Boolean) {
        backgroundSyncValue = backgroundSync
    }

    /** Kotlin property syntax, equivalent to [isBackgroundSync] / [setBackgroundSync]. */
    public var isBackgroundSync: Boolean
        @JvmSynthetic @JvmName("ktIsBackgroundSync") inline get() = isBackgroundSync()
        @JvmSynthetic @JvmName("ktSetBackgroundSync") inline set(value) = setBackgroundSync(value)

    public open fun isCreatedPostAppLaunch(): Boolean = createdPostAppLaunch

    /** Kotlin property syntax, equivalent to [isCreatedPostAppLaunch]. */
    public val isCreatedPostAppLaunch: Boolean
        @JvmSynthetic @JvmName("ktIsCreatedPostAppLaunch") inline get() = isCreatedPostAppLaunch()

    /**
     * Whether the App Launched event is disabled. The getter was package-private in Java and is
     * SDK-internal (`@RestrictTo(LIBRARY)`); the setter is public API.
     */
    @RestrictTo(Scope.LIBRARY)
    public open fun isDisableAppLaunchedEvent(): Boolean = disableAppLaunchedEventValue

    public open fun setDisableAppLaunchedEvent(disableAppLaunchedEvent: Boolean) {
        disableAppLaunchedEventValue = disableAppLaunchedEvent
    }

    /** Kotlin property syntax, equivalent to [isDisableAppLaunchedEvent] / [setDisableAppLaunchedEvent]. */
    public var isDisableAppLaunchedEvent: Boolean
        @JvmSynthetic @JvmName("ktIsDisableAppLaunchedEvent") inline get() = isDisableAppLaunchedEvent()
        @JvmSynthetic @JvmName("ktSetDisableAppLaunchedEvent") inline set(value) = setDisableAppLaunchedEvent(value)

    @get:JvmName("isPersonalizationEnabled")
    internal val isPersonalizationEnabled: Boolean
        get() = personalization

    public open fun isSslPinningEnabled(): Boolean = sslPinning

    /** Kotlin property syntax, equivalent to [isSslPinningEnabled]. */
    public val isSslPinningEnabled: Boolean
        @JvmSynthetic @JvmName("ktIsSslPinningEnabled") inline get() = isSslPinningEnabled()

    @get:JvmName("isUseGoogleAdId")
    internal val isUseGoogleAdId: Boolean
        get() = useGoogleAdId

    @JvmName("setCreatedPostAppLaunch")
    internal fun setCreatedPostAppLaunch() {
        createdPostAppLaunch = true
    }

    public open fun setEncryptionLevel(encryptionLevel: EncryptionLevel) {
        encryptionLevelValue = encryptionLevel.intValue()
    }

    public open fun getEncryptionLevel(): Int = encryptionLevelValue

    /** Kotlin property syntax, equivalent to [getEncryptionLevel]. */
    public val encryptionLevel: Int
        @JvmSynthetic @JvmName("ktGetEncryptionLevel") inline get() = getEncryptionLevel()

    public open fun setEncryptionInTransit(encryptionInTransit: Boolean) {
        encryptionInTransitValue = if (encryptionInTransit) { "1" } else { "0" }
    }

    public open fun isEncryptionInTransitEnabled(): Boolean = try {
        // a null value fails to parse, as it did in Java
        encryptionInTransitValue.orEmpty().toInt() > 0
    } catch (e: NumberFormatException) {
        Logger.v("Invalid value passed in manifest for encryption in transit")
        false
    }

    /** Kotlin property syntax, equivalent to [isEncryptionInTransitEnabled]. */
    public val isEncryptionInTransitEnabled: Boolean
        @JvmSynthetic @JvmName("ktIsEncryptionInTransitEnabled") inline get() = isEncryptionInTransitEnabled()

    @JvmName("toJSONString")
    internal fun toJSONString(): String? = try {
        JSONObject().apply {
            put(KEY_ACCOUNT_ID, accountIdValue)
            put(KEY_ACCOUNT_TOKEN, accountTokenValue)
            put(KEY_ACCOUNT_REGION, accountRegion)
            put(KEY_PROXY_DOMAIN, proxyDomain)
            put(KEY_SPIKY_PROXY_DOMAIN, spikyProxyDomain)
            put(KEY_CUSTOM_HANDSHAKE_DOMAIN, customHandshakeDomain)
            put(KEY_FCM_SENDER_ID, fcmSenderId)
            put(KEY_ANALYTICS_ONLY, isAnalyticsOnly)
            put(KEY_DEFAULT_INSTANCE, isDefaultInstance)
            put(KEY_USE_GOOGLE_AD_ID, isUseGoogleAdId)
            put(KEY_DISABLE_APP_LAUNCHED, isDisableAppLaunchedEvent)
            put(KEY_PERSONALIZATION, isPersonalizationEnabled)
            put(KEY_DEBUG_LEVEL, debugLevel)
            put(KEY_CREATED_POST_APP_LAUNCH, isCreatedPostAppLaunch)
            put(KEY_SSL_PINNING, isSslPinningEnabled)
            put(KEY_BACKGROUND_SYNC, isBackgroundSync)
            put(KEY_ENABLE_CUSTOM_CT_ID, enableCustomCleverTapId)
            put(KEY_PACKAGE_NAME, packageName)
            put(KEY_BETA, isBeta)
            put(KEY_ENCRYPTION_LEVEL, encryptionLevel)
            put(KEY_ENCRYPTION_IN_TRANSIT, encryptionInTransitValue)
            put(KEY_PUSH_TYPES, pushTypesJson())
        }.toString()
    } catch (e: Throwable) {
        Logger.v("Unable to convert config to JSON : ", e.cause)
        null
    }

    private fun pushTypesJson(): JSONArray = JSONArray().apply {
        // fcm is always loaded by default.
        pushTypes.filter { it !== PushConstants.FCM }.forEach { put(it.toJSONObject()) }
    }

    private fun defaultSuffix(tag: String): String =
        "[${if (tag.isNotEmpty()) { ":$tag" } else { "" }}:$accountIdValue]"

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<CleverTapInstanceConfig> =
            object : Parcelable.Creator<CleverTapInstanceConfig> {
                override fun createFromParcel(parcel: Parcel): CleverTapInstanceConfig = CleverTapInstanceConfig(parcel)

                override fun newArray(size: Int): Array<CleverTapInstanceConfig?> = arrayOfNulls(size)
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
        public const val KEY_ENCRYPTION_LEVEL: String = "encryptionLevel"
        private const val KEY_PUSH_TYPES = "allowedPushTypes"
        private const val KEY_ENCRYPTION_IN_TRANSIT = "encryptionInTransit"

        /**
         * Creates a CleverTapInstanceConfig with meta data from manifest file
         * @param context Application context
         * @return CleverTapInstanceConfig, or `null` if the account ID or token is missing from the manifest
         */
        @JvmStatic
        public fun getDefaultInstance(context: Context?): CleverTapInstanceConfig? {
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
                info.accountRegion,
                true
            )
        }

        /**
         * Creates a CleverTapInstanceConfig for an additional instance.
         *
         * @return the config, or `null` if [accountId] or [accountToken] is null
         */
        @JvmStatic
        public fun createInstance(
            context: Context?,
            accountId: String?,
            accountToken: String?
        ): CleverTapInstanceConfig? = createInstance(context, accountId, accountToken, null)

        /**
         * Creates a CleverTapInstanceConfig for an additional instance.
         *
         * @return the config, or `null` if [accountId] or [accountToken] is null
         */
        @JvmStatic
        public fun createInstance(
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
        ): CleverTapInstanceConfig =
            createInstanceWithManifest(ManifestInfo.getInstance(context), accountId, accountToken, accountRegion, true)

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
        ): CleverTapInstanceConfig =
            CleverTapInstanceConfig(manifest, accountId, accountToken, accountRegion, isDefaultInstance)

        // for internal use only!
        @JvmStatic
        protected fun createInstance(jsonString: String?): CleverTapInstanceConfig? = createInstanceFromJson(jsonString)

        /**
         * SDK-internal access to [createInstance] (String): Kotlin `protected` companion members are
         * not visible to other classes in the package, unlike Java `protected static`.
         */
        internal fun createInstanceFromJson(jsonString: String?): CleverTapInstanceConfig? = try {
            CleverTapInstanceConfig(jsonString)
        } catch (t: Throwable) {
            null
        }

        private fun Parcel.readFlag(): Boolean = readByte().toInt() != 0x00

        private fun Parcel.writeFlag(value: Boolean) = writeByte(if (value) { 0x01 } else { 0x00 })
    }
}
