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
public open class ManifestInfo(
    public val accountId: String?,
    public val accountToken: String?,
    accountRegion: String?,
    proxyDomain: String?,
    spikyProxyDomain: String?,
    handshakeDomain: String?,
    private val useADID: Boolean,
    public val isAppLaunchedDisabled: Boolean,
    public val notificationIcon: String?,
    public val excludedActivities: String?,
    private val sslPinning: Boolean,
    public val isBackgroundSync: Boolean,
    private val useCustomID: Boolean,
    @get:JvmName("getFCMSenderId")
    public val fcmSenderId: String?,
    public val packageName: String?,
    private val beta: Boolean,
    public val intentServiceName: String?,
    public val devDefaultPushChannelId: String?,
    public val profileKeys: Array<String>?,
    public val encryptionLevel: Int,
    public val vendorOneProvider: String?,
    public val vendorTwoProvider: String?,
    public val encryptionInTransit: String?,
    /**
     * Whether custom-html header/footer in-apps may fall back to a WindowManager overlay when the
     * host Activity is not a [androidx.fragment.app.FragmentActivity]. Opt-in (default false),
     * enabled by the gaming wrapper SDKs via the `CLEVERTAP_INAPP_FRAGMENTLESS_BANNERS`
     * manifest flag; only set from the manifest.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val isFragmentlessInAppBannersEnabled: Boolean = false
) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val accountRegion: String? = accountRegion
        get() {
            Logger.v("ManifestInfo: getAccountRegion called, returning region:$field")
            return field
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val proxyDomain: String? = proxyDomain
        get() {
            Logger.v("ManifestInfo: getProxyDomain called, returning proxyDomain:$field")
            return field
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val spikeyProxyDomain: String? = spikyProxyDomain
        get() {
            Logger.v("ManifestInfo: getSpikeyProxyDomain called, returning spikeyProxyDomain:$field")
            return field
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val handshakeDomain: String? = handshakeDomain
        get() {
            Logger.v("ManifestInfo: getHandshakeDomain called, returning handshakeDomain:$field")
            return field
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val isSSLPinningEnabled: Boolean
        get() = sslPinning

    public fun enableBeta(): Boolean = beta

    public fun useCustomId(): Boolean = useCustomID

    public fun useGoogleAdId(): Boolean = useADID

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {

        private const val LABEL_ACCOUNT_ID = "CLEVERTAP_ACCOUNT_ID"
        private const val LABEL_TOKEN = "CLEVERTAP_TOKEN"
        public const val LABEL_NOTIFICATION_ICON: String = "CLEVERTAP_NOTIFICATION_ICON"
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
        public const val LABEL_MODULE_SDK_VERSION_PREFIX: String = "com.clevertap.sdk.version."

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
        // @JvmStatic: Signed Call and push-templates <= 2.7.0 link `invokestatic ManifestInfo.getInstance`.
        @JvmStatic
        public fun getInstance(context: Context?): ManifestInfo = synchronized(ManifestInfo::class.java) {
            instance ?: fromManifest(context).also { instance = it }
        }

        // Only added for testing
        @TestOnly
        public fun clearPreloadedManifestInfo() {
            instance = null
        }

        public fun changeCredentials(id: String?, token: String?, region: String?) {
            ccAccountId = id
            ccAccountToken = token
            ccAccountRegion = region
        }

        public fun changeCredentials(id: String?, token: String?, proxyDomain: String?, spikyProxyDomain: String?) {
            ccAccountId = id
            ccAccountToken = token
            ccProxyDomain = proxyDomain
            ccSpikyProxyDomain = spikyProxyDomain
        }

        public fun changeCredentials(
            id: String?,
            token: String?,
            proxyDomain: String?,
            spikyProxyDomain: String?,
            customHandshakeDomain: String?
        ) {
            ccAccountId = id
            ccAccountToken = token
            ccProxyDomain = proxyDomain
            ccSpikyProxyDomain = spikyProxyDomain
            ccHandshakeDomain = customHandshakeDomain
        }

        private fun fromManifest(context: Context?): ManifestInfo {
            val metaData = readMetaData(context)

            fun value(key: String) = manifestStringValue(metaData, key)
            fun flag(key: String) = "1" == value(key)

            val manifestInfo = ManifestInfo(
                // credentials passed to changeCredentials win over the manifest
                accountId = ccAccountId ?: value(LABEL_ACCOUNT_ID),
                accountToken = ccAccountToken ?: value(LABEL_TOKEN),
                accountRegion = ccAccountRegion ?: value(LABEL_REGION),
                proxyDomain = ccProxyDomain ?: value(LABEL_PROXY_DOMAIN),
                spikyProxyDomain = ccSpikyProxyDomain ?: value(LABEL_SPIKY_PROXY_DOMAIN),
                handshakeDomain = ccHandshakeDomain ?: value(LABEL_CLEVERTAP_HANDSHAKE_DOMAIN),
                notificationIcon = value(LABEL_NOTIFICATION_ICON),
                useADID = flag(LABEL_USE_GOOGLE_AD_ID),
                isAppLaunchedDisabled = flag(LABEL_DISABLE_APP_LAUNCH),
                excludedActivities = value(LABEL_INAPP_EXCLUDE),
                sslPinning = flag(LABEL_SSL_PINNING),
                isBackgroundSync = flag(LABEL_BACKGROUND_SYNC),
                useCustomID = flag(LABEL_CUSTOM_ID),
                fcmSenderId = value(LABEL_FCM_SENDER_ID)?.replace("id:", ""),
                encryptionLevel = parseEncryptionLevel(value(LABEL_ENCRYPTION_LEVEL)),
                packageName = value(LABEL_PACKAGE_NAME),
                beta = flag(LABEL_BETA),
                isFragmentlessInAppBannersEnabled = flag(LABEL_INAPP_FRAGMENTLESS_BANNERS),
                intentServiceName = value(LABEL_INTENT_SERVICE),
                devDefaultPushChannelId = value(LABEL_DEFAULT_CHANNEL_ID),
                profileKeys = parseProfileKeys(value(Constants.CLEVERTAP_IDENTIFIER)),
                vendorOneProvider = value(LABEL_PUSH_PROVIDER_1),
                vendorTwoProvider = value(LABEL_PUSH_PROVIDER_2),
                encryptionInTransit = value(LABEL_ENCRYPTION_IN_TRANSIT)
            )
            registerModuleSdkVersions(metaData)
            return manifestInfo
        }

        private fun readMetaData(context: Context?): Bundle {
            return try {
                // A null context throws here and is swallowed, exactly as the Java version did.
                val ctx = requireNotNull(context)
                ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA).metaData
            } catch (t: Throwable) {
                null
            } ?: Bundle()
        }

        private fun parseEncryptionLevel(manifestEncryptionLevel: String?): Int = try {
            val level = manifestEncryptionLevel?.toInt() ?: 0
            if (level in 0..2) {
                level
            } else {
                Logger.v("Invalid encryption level is used, defaulting to no encryption")
                0
            }
        } catch (t: Throwable) {
            Logger.v("Unable to parse encryption level from the Manifest, Setting it to 0 by default", t.cause)
            0
        }

        private fun parseProfileKeys(profileKeyString: String?): Array<String> =
            // Pattern#split matches the former String#split(regex) semantics (trailing empties dropped).
            if (!profileKeyString.isNullOrEmpty()) {
                Pattern.compile(Constants.SEPARATOR_COMMA).split(profileKeyString)
            } else {
                Constants.NULL_STRING_ARRAY
            }

        /**
         * This returns string representation of int,boolean,string,float value of given key
         *
         * @param manifest bundle to retrieve values from
         * @param name     key of bundle
         * @return string representation of int,boolean,string,float
         */
        @Suppress("DEPRECATION")
        private fun manifestStringValue(manifest: Bundle, name: String): String? = try {
            manifest.get(name)?.toString()
        } catch (t: Throwable) {
            null
        }

        public fun registerModuleSdkVersions(metaData: Bundle) {
            try {
                for (key in metaData.keySet()) {
                    registerModuleSdkVersion(metaData = metaData, key = key)
                }
            } catch (t: Throwable) {
                Logger.v("Unable to read module SDK versions from the manifest", t)
            }
        }

        private fun registerModuleSdkVersion(metaData: Bundle, key: String?) {
            if (key == null || !key.startsWith(LABEL_MODULE_SDK_VERSION_PREFIX)) {
                return
            }
            val name = key.substring(LABEL_MODULE_SDK_VERSION_PREFIX.length)
            @Suppress("DEPRECATION")
            val value = metaData.get(key)
            if (name.isEmpty() || value == null) {
                return
            }
            try {
                // A numeric android:value is stored as an Integer, anything else as a String
                val version = if (value is Int) { value } else { value.toString().toInt() }
                CoreMetaData.setSharedCustomSdkVersion(name, version)
            } catch (e: NumberFormatException) {
                Logger.v("Ignoring non numeric SDK version in manifest for $key")
            }
        }
    }
}
