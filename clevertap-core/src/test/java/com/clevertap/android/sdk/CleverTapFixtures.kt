package com.clevertap.android.sdk

import com.clevertap.android.shared.test.Constant

class CleverTapFixtures {

    companion object {

        /**
         * Creates a ManifestInfo instance with default parameters for testing.
         * 
         * @param accountId Account ID (default: Constant.ACC_ID)
         * @param accountToken Account token (default: Constant.ACC_TOKEN)
         * @param accountRegion Account region (default: null)
         * @param proxyDomain Proxy domain (default: "")
         * @param spikyProxyDomain Spiky proxy domain (default: "")
         * @param handshakeDomain Handshake domain (default: "")
         * @param useADID Use Google Ad ID (default: false)
         * @param appLaunchedDisabled App launched disabled (default: false)
         * @param notificationIcon Notification icon (default: "notification icon")
         * @param excludedActivitiesForInApps Excluded activities for in-apps (default: null)
         * @param sslPinning SSL pinning enabled (default: false)
         * @param backgroundSync Background sync enabled (default: false)
         * @param useCustomID Use custom ID (default: false)
         * @param fcmSenderId FCM sender ID (default: "fcm:sender:id")
         * @param packageName Package name (default: "some.app.package")
         * @param beta Beta mode enabled (default: false)
         * @param intentServiceName Intent service name (default: "serviceName")
         * @param devDefaultPushChannelId Default push channel ID (default: "push-channel-id")
         * @param profileKeys Profile keys array (default: emptyArray())
         * @param encryptionLevel Encryption level (default: 0)
         * @param provider1 Provider 1 configuration (default: HMS configuration)
         * @param provider2 Provider 2 configuration (default: null)
         * @param encryptionInTransit Encryption in transit (default: "0")
         */
        fun createManifestInfo(
            accountId: String = Constant.ACC_ID,
            accountToken: String = Constant.ACC_TOKEN,
            accountRegion: String? = null,
            proxyDomain: String = "",
            spikyProxyDomain: String = "",
            handshakeDomain: String = "",
            useADID: Boolean = false,
            appLaunchedDisabled: Boolean = false,
            notificationIcon: String = "notification icon",
            excludedActivitiesForInApps: String? = null,
            sslPinning: Boolean = false,
            backgroundSync: Boolean = false,
            useCustomID: Boolean = false,
            fcmSenderId: String = "fcm:sender:id",
            packageName: String = "some.app.package",
            beta: Boolean = false,
            intentServiceName: String = "serviceName",
            devDefaultPushChannelId: String = "push-channel-id",
            profileKeys: Array<String> = emptyArray(),
            encryptionLevel: Int = 0,
            provider1: String = "hps,hps_token,com.clevertap.android.hms.HmsPushProvider,com.huawei.hms.push.HmsMessageService",
            provider2: String? = null,
            encryptionInTransit: String = "0"
        ): ManifestInfo {
            return ManifestInfo(
                accountId,
                accountToken,
                accountRegion,
                proxyDomain,
                spikyProxyDomain,
                handshakeDomain,
                useADID,
                appLaunchedDisabled,
                notificationIcon,
                excludedActivitiesForInApps,
                sslPinning,
                backgroundSync,
                useCustomID,
                fcmSenderId,
                packageName,
                beta,
                intentServiceName,
                devDefaultPushChannelId,
                profileKeys,
                encryptionLevel,
                provider1,
                provider2,
                encryptionInTransit
            )
        }

        val manifestInfo = createManifestInfo()

        fun provideCleverTapInstanceConfig(): CleverTapInstanceConfig =
            CleverTapInstanceConfig.createInstanceWithManifest(
                manifestInfo,
                Constant.ACC_ID,
                Constant.ACC_TOKEN,
                null,
                true
            )
    }
}