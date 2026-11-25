package com.clevertap.android.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.RestrictTo
import java.util.regex.Pattern
import org.jetbrains.annotations.TestOnly

/**
 * Parser for android manifest and picks up fields from manifest once to be references
 *
 * Should be singleton and initialised only once -> need to validate.
 */
// todo lp Remove context dependency from here
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class ManifestInfo {

    val accountId: String?
    val accountToken: String?
    private val accountRegion: String?
    private val proxyDomain: String?
    private val spikyProxyDomain: String?
    private val handshakeDomain: String?
    private val useADID: Boolean
    val isAppLaunchedDisabled: Boolean
    val notificationIcon: String?
    private val excludedActivitiesForInApps: String?
    private val sslPinning: Boolean
    val isBackgroundSync: Boolean
    private val useCustomID: Boolean

    @get:JvmName("getFCMSenderId")
    val fcmSenderId: String?
    val packageName: String?
    private val beta: Boolean
    private val fragmentlessInAppBanners: Boolean
    val intentServiceName: String?
    val devDefaultPushChannelId: String?
    val profileKeys: Array<String>?
    val encryptionLevel: Int
    private val provider1: String?
    private val provider2: String?
    val encryptionInTransit: String?

    private constructor(context: Context?) {
        var metaData: Bundle? = null
        try {
            // A null context throws here and is swallowed, exactly as the Java version did.
            val pm = context!!.packageManager
            val ai = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            metaData = ai.metaData
        } catch (t: Throwable) {
            // no-op
        }
        if (metaData == null) {
            metaData = Bundle()
        }

        // start -> assign these if they did not happen in changeCredentials
        accountId = ccAccountId ?: _getManifestStringValueForKey(metaData, LABEL_ACCOUNT_ID)
        accountToken = ccAccountToken ?: _getManifestStringValueForKey(metaData, LABEL_TOKEN)
        accountRegion = ccAccountRegion ?: _getManifestStringValueForKey(metaData, LABEL_REGION)
        proxyDomain = ccProxyDomain ?: _getManifestStringValueForKey(metaData, LABEL_PROXY_DOMAIN)
        spikyProxyDomain = ccSpikyProxyDomain ?: _getManifestStringValueForKey(metaData, LABEL_SPIKY_PROXY_DOMAIN)
        handshakeDomain = ccHandshakeDomain ?: _getManifestStringValueForKey(metaData, LABEL_CLEVERTAP_HANDSHAKE_DOMAIN)
        // end -> assign these if they did not happen in changeCredentials

        notificationIcon = _getManifestStringValueForKey(metaData, LABEL_NOTIFICATION_ICON)
        useADID = "1" == _getManifestStringValueForKey(metaData, LABEL_USE_GOOGLE_AD_ID)
        isAppLaunchedDisabled = "1" == _getManifestStringValueForKey(metaData, LABEL_DISABLE_APP_LAUNCH)
        excludedActivitiesForInApps = _getManifestStringValueForKey(metaData, LABEL_INAPP_EXCLUDE)
        sslPinning = "1" == _getManifestStringValueForKey(metaData, LABEL_SSL_PINNING)
        isBackgroundSync = "1" == _getManifestStringValueForKey(metaData, LABEL_BACKGROUND_SYNC)
        useCustomID = "1" == _getManifestStringValueForKey(metaData, LABEL_CUSTOM_ID)

        var fcmSenderIdTemp: String?
        fcmSenderIdTemp = _getManifestStringValueForKey(metaData, LABEL_FCM_SENDER_ID)
        if (fcmSenderIdTemp != null) {
            fcmSenderIdTemp = fcmSenderIdTemp.replace("id:", "")
        }
        fcmSenderId = fcmSenderIdTemp

        var encLvlTemp: Int
        try {
            val manifestEncryptionLevel = _getManifestStringValueForKey(metaData, LABEL_ENCRYPTION_LEVEL)
            var parsedEncryptionLevel = 0
            if (manifestEncryptionLevel != null) {
                parsedEncryptionLevel = Integer.parseInt(manifestEncryptionLevel)
            }

            if (parsedEncryptionLevel >= 0 && parsedEncryptionLevel <= 2) {
                encLvlTemp = parsedEncryptionLevel
            } else {
                Logger.v("Invalid encryption level is used, defaulting to no encryption")
                encLvlTemp = 0
            }
        } catch (t: Throwable) {
            encLvlTemp = 0
            Logger.v("Unable to parse encryption level from the Manifest, Setting it to 0 by default", t.cause)
        }
        encryptionLevel = encLvlTemp

        packageName = _getManifestStringValueForKey(metaData, LABEL_PACKAGE_NAME)
        beta = "1" == _getManifestStringValueForKey(metaData, LABEL_BETA)
        fragmentlessInAppBanners = "1" == _getManifestStringValueForKey(metaData, LABEL_INAPP_FRAGMENTLESS_BANNERS)
        intentServiceName = _getManifestStringValueForKey(metaData, LABEL_INTENT_SERVICE)
        devDefaultPushChannelId = _getManifestStringValueForKey(metaData, LABEL_DEFAULT_CHANNEL_ID)
        profileKeys = parseProfileKeys(metaData)
        provider1 = _getManifestStringValueForKey(metaData, LABEL_PUSH_PROVIDER_1)
        provider2 = _getManifestStringValueForKey(metaData, LABEL_PUSH_PROVIDER_2)
        encryptionInTransit = _getManifestStringValueForKey(metaData, LABEL_ENCRYPTION_IN_TRANSIT)
        registerModuleSdkVersions(metaData)
    }

    constructor(
        accountId: String?,
        accountToken: String?,
        accountRegion: String?,
        proxyDomain: String?,
        spikyProxyDomain: String?,
        handshakeDomain: String?,
        useADID: Boolean,
        appLaunchedDisabled: Boolean,
        notificationIcon: String?,
        excludedActivitiesForInApps: String?,
        sslPinning: Boolean,
        backgroundSync: Boolean,
        useCustomID: Boolean,
        fcmSenderId: String?,
        packageName: String?,
        beta: Boolean,
        intentServiceName: String?,
        devDefaultPushChannelId: String?,
        profileKeys: Array<String>?,
        encryptionLevel: Int,
        provider1: String?,
        provider2: String?,
        encryptionInTransit: String?
    ) {
        // assign these if they did not happen in change creds
        this.accountId = accountId
        this.accountToken = accountToken
        this.accountRegion = accountRegion
        this.proxyDomain = proxyDomain
        this.spikyProxyDomain = spikyProxyDomain
        this.handshakeDomain = handshakeDomain

        this.useADID = useADID
        this.isAppLaunchedDisabled = appLaunchedDisabled
        this.notificationIcon = notificationIcon
        this.excludedActivitiesForInApps = excludedActivitiesForInApps
        this.sslPinning = sslPinning
        this.isBackgroundSync = backgroundSync
        this.useCustomID = useCustomID
        this.fcmSenderId = fcmSenderId
        this.packageName = packageName
        this.beta = beta
        // Not exposed through this explicit constructor; only set via the manifest flag.
        this.fragmentlessInAppBanners = false
        this.intentServiceName = intentServiceName
        this.devDefaultPushChannelId = devDefaultPushChannelId
        this.profileKeys = profileKeys
        this.encryptionLevel = encryptionLevel
        this.provider1 = provider1
        this.provider2 = provider2
        this.encryptionInTransit = encryptionInTransit
    }

    val excludedActivities: String?
        get() = excludedActivitiesForInApps

    fun enableBeta(): Boolean {
        return beta
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getAccountRegion(): String? {
        Logger.v("ManifestInfo: getAccountRegion called, returning region:$accountRegion")
        return accountRegion
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getProxyDomain(): String? {
        Logger.v("ManifestInfo: getProxyDomain called, returning proxyDomain:$proxyDomain")
        return proxyDomain
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSpikeyProxyDomain(): String? {
        Logger.v("ManifestInfo: getSpikeyProxyDomain called, returning spikeyProxyDomain:$spikyProxyDomain")
        return spikyProxyDomain
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getHandshakeDomain(): String? {
        Logger.v("ManifestInfo: getHandshakeDomain called, returning handshakeDomain:$handshakeDomain")
        return handshakeDomain
    }

    /**
     * Whether custom-html header/footer in-apps may fall back to a WindowManager overlay when the
     * host Activity is not a [androidx.fragment.app.FragmentActivity]. Opt-in (default false),
     * enabled by the gaming wrapper SDKs via the `CLEVERTAP_INAPP_FRAGMENTLESS_BANNERS`
     * manifest flag.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isFragmentlessInAppBannersEnabled: Boolean
        get() = fragmentlessInAppBanners

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val isSSLPinningEnabled: Boolean
        get() = sslPinning

    fun useCustomId(): Boolean {
        return useCustomID
    }

    fun useGoogleAdId(): Boolean {
        return useADID
    }

    private fun parseProfileKeys(metaData: Bundle): Array<String> {
        val profileKeyString = _getManifestStringValueForKey(metaData, Constants.CLEVERTAP_IDENTIFIER)
        // Pattern#split matches the former String#split(regex) semantics (trailing empties dropped).
        return if (!profileKeyString.isNullOrEmpty()) {
            Pattern.compile(Constants.SEPARATOR_COMMA).split(profileKeyString)
        } else {
            Constants.NULL_STRING_ARRAY
        }
    }

    /**
     * This returns string representation of int,boolean,string,float value of given key
     *
     * @param manifest bundle to retrieve values from
     * @param name     key of bundle
     * @return string representation of int,boolean,string,float
     */
    @Suppress("DEPRECATION")
    private fun _getManifestStringValueForKey(manifest: Bundle, name: String): String? {
        return try {
            val o = manifest.get(name)
            o?.toString()
        } catch (t: Throwable) {
            null
        }
    }

    val vendorOneProvider: String?
        get() = provider1

    val vendorTwoProvider: String?
        get() = provider2

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        private const val LABEL_ACCOUNT_ID = "CLEVERTAP_ACCOUNT_ID"
        private const val LABEL_TOKEN = "CLEVERTAP_TOKEN"
        const val LABEL_NOTIFICATION_ICON: String = "CLEVERTAP_NOTIFICATION_ICON"
        private const val LABEL_INAPP_EXCLUDE = "CLEVERTAP_INAPP_EXCLUDE"
        private const val LABEL_REGION = "CLEVERTAP_REGION"
        private const val LABEL_PROXY_DOMAIN = "CLEVERTAP_PROXY_DOMAIN"
        private const val LABEL_SPIKY_PROXY_DOMAIN = "CLEVERTAP_SPIKY_PROXY_DOMAIN"
        private const val LABEL_CLEVERTAP_HANDSHAKE_DOMAIN = "CLEVERTAP_HANDSHAKE_DOMAIN"
        private const val LABEL_DISABLE_APP_LAUNCH = "CLEVERTAP_DISABLE_APP_LAUNCHED"
        private const val LABEL_SSL_PINNING = "CLEVERTAP_SSL_PINNING"
        private const val LABEL_BACKGROUND_SYNC = "CLEVERTAP_BACKGROUND_SYNC"
        private const val LABEL_CUSTOM_ID = "CLEVERTAP_USE_CUSTOM_ID"
        private const val LABEL_USE_GOOGLE_AD_ID = "CLEVERTAP_USE_GOOGLE_AD_ID"
        private const val LABEL_FCM_SENDER_ID = "FCM_SENDER_ID"
        private const val LABEL_PACKAGE_NAME = "CLEVERTAP_APP_PACKAGE"
        private const val LABEL_BETA = "CLEVERTAP_BETA"
        private const val LABEL_INTENT_SERVICE = "CLEVERTAP_INTENT_SERVICE"
        private const val LABEL_ENCRYPTION_LEVEL = "CLEVERTAP_ENCRYPTION_LEVEL"
        private const val LABEL_DEFAULT_CHANNEL_ID = "CLEVERTAP_DEFAULT_CHANNEL_ID"

        // intentionally named this way to avoid static scan flagging on codebase
        private const val LABEL_PUSH_PROVIDER_1 = "CLEVERTAP_PROVIDER_1"
        private const val LABEL_PUSH_PROVIDER_2 = "CLEVERTAP_PROVIDER_2"

        private const val LABEL_ENCRYPTION_IN_TRANSIT = "CLEVERTAP_ENCRYPTION_IN_TRANSIT"

        // Opt-in (default off) that lets custom-html header/footer in-apps render via a WindowManager
        // overlay when the host Activity is not a FragmentActivity. Enabled by the gaming wrapper SDKs
        // (Unreal/Unity) via their merged manifest; regular integrations never set it.
        private const val LABEL_INAPP_FRAGMENTLESS_BANNERS = "CLEVERTAP_INAPP_FRAGMENTLESS_BANNERS"

        // Module versions from manifest meta-data, sent in af even if the app never uses the module
        const val LABEL_MODULE_SDK_VERSION_PREFIX: String = "com.clevertap.sdk.version."

        private var instance: ManifestInfo? = null // singleton

        // Start - Capture the credentials from ChangeCredentials
        private var ccAccountId: String? = null
        private var ccAccountToken: String? = null
        private var ccAccountRegion: String? = null
        private var ccProxyDomain: String? = null
        private var ccSpikyProxyDomain: String? = null
        private var ccHandshakeDomain: String? = null
        // End - Capture the credentials from ChangeCredentials

        // Locks on ManifestInfo.class, the monitor the former `static synchronized` method used.
        @JvmStatic
        fun getInstance(context: Context?): ManifestInfo {
            synchronized(ManifestInfo::class.java) {
                if (instance == null) {
                    instance = ManifestInfo(context)
                }
                return instance!!
            }
        }

        // Only added for testing
        @TestOnly
        @JvmStatic
        fun clearPreloadedManifestInfo() {
            instance = null
        }

        @JvmStatic
        fun changeCredentials(id: String?, token: String?, region: String?) {
            ccAccountId = id
            ccAccountToken = token
            ccAccountRegion = region
        }

        @JvmStatic
        fun changeCredentials(id: String?, token: String?, _proxyDomain: String?, _spikyProxyDomain: String?) {
            ccAccountId = id
            ccAccountToken = token
            ccProxyDomain = _proxyDomain
            ccSpikyProxyDomain = _spikyProxyDomain
        }

        @JvmStatic
        fun changeCredentials(
            id: String?,
            token: String?,
            _proxyDomain: String?,
            _spikyProxyDomain: String?,
            customHandshakeDomain: String?
        ) {
            ccAccountId = id
            ccAccountToken = token
            ccProxyDomain = _proxyDomain
            ccSpikyProxyDomain = _spikyProxyDomain
            ccHandshakeDomain = customHandshakeDomain
        }

        @JvmStatic
        fun registerModuleSdkVersions(metaData: Bundle) {
            try {
                for (key in metaData.keySet()) {
                    if (key == null || !key.startsWith(LABEL_MODULE_SDK_VERSION_PREFIX)) {
                        continue
                    }
                    val name = key.substring(LABEL_MODULE_SDK_VERSION_PREFIX.length)
                    @Suppress("DEPRECATION")
                    val value = metaData.get(key)
                    if (name.isEmpty() || value == null) {
                        continue
                    }
                    try {
                        // A numeric android:value is stored as an Integer, anything else as a String
                        val version = if (value is Int) value else Integer.parseInt(value.toString())
                        CoreMetaData.setSharedCustomSdkVersion(name, version)
                    } catch (e: NumberFormatException) {
                        Logger.v("Ignoring non numeric SDK version in manifest for $key")
                    }
                }
            } catch (t: Throwable) {
                Logger.v("Unable to read module SDK versions from the manifest", t)
            }
        }
    }
}
