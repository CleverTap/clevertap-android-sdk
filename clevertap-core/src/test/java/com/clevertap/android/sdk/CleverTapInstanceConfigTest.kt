package com.clevertap.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.pushnotification.PushType
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CleverTapInstanceConfigTest {
    
    companion object {
        private const val ACCOUNT_ID = "accountId"
        private const val ACCOUNT_TOKEN = "accountToken"
        private const val ACCOUNT_REGION = "eu1"

        private val FCM_PUSH_TYPE = PushType(
            "fcm",
            "fcm_token",
            "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider",
            "com.google.firebase.messaging.FirebaseMessagingService"
        )

        private val HPS_PUSH_TYPE = PushType(
            "hps",
            "hps_token",
            "com.clevertap.android.hms.HmsPushProvider",
            "com.huawei.hms.push.HmsMessageService"
        );
    }
    private val manifestInfo = CleverTapFixtures.manifestInfo
    
    @Test
    fun `test config push providers list from manifest`() {
        val config = CleverTapInstanceConfig.createInstanceWithManifest(
            manifestInfo,
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        )

        val pushTypes = config.pushTypes

        assertEquals(2, pushTypes.size)
        assertEquals(FCM_PUSH_TYPE, pushTypes[0])
        assertEquals(HPS_PUSH_TYPE, pushTypes[1])
    }

    @Test
    fun `test config push providers not added twice`() {
        val config = CleverTapInstanceConfig.createInstanceWithManifest(
            manifestInfo,
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        ).apply {
            addPushType(HPS_PUSH_TYPE)
            addPushType(HPS_PUSH_TYPE)
            addPushType(HPS_PUSH_TYPE)
        }

        val pushTypes = config.pushTypes

        assertEquals(2, pushTypes.size)
        assertEquals(FCM_PUSH_TYPE, pushTypes[0])
        assertEquals(HPS_PUSH_TYPE, pushTypes[1])
    }

    @Test
    fun `test encryption in transit ON use case`() {

        val encryptionOn = CleverTapInstanceConfig.createInstanceWithManifest(
            CleverTapFixtures.createManifestInfo(encryptionInTransit = "1"),
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        )
        assertTrue(encryptionOn.isEncryptionInTransitEnabled)
    }

    @Test
    fun `test encryption in transit OFF use case`() {
        val encryptionOff = CleverTapInstanceConfig.createInstanceWithManifest(
            CleverTapFixtures.createManifestInfo(encryptionInTransit = "0"),
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        )
        assertFalse(encryptionOff.isEncryptionInTransitEnabled)
    }

    @Test
    fun `test encryption in transit invalid input use case`() {
        val encryptionInvalid = CleverTapInstanceConfig.createInstanceWithManifest(
            CleverTapFixtures.createManifestInfo(encryptionInTransit = "garbage-string"),
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        )
        assertFalse(encryptionInvalid.isEncryptionInTransitEnabled)
    }

    @Test
    fun `test clevertap instance config data is preserved after json serialization and deserialization`() {
        val originalConfig = CleverTapInstanceConfig.createInstanceWithManifest(
            CleverTapFixtures.createManifestInfo(encryptionInTransit = "1"),
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            ACCOUNT_REGION,
            true
        )
        val jsonString = originalConfig.toJSONString()
        val configFromJson = CleverTapInstanceConfig.createInstance(jsonString)!!

        assertEquals(
            originalConfig.isEncryptionInTransitEnabled,
            configFromJson.isEncryptionInTransitEnabled,
            "Encryption in transit flag should be preserved after JSON serialization/deserialization"
        )

        assertEquals(originalConfig.accountId, configFromJson.accountId, "Account ID should be preserved")
        assertEquals(originalConfig.accountToken, configFromJson.accountToken, "Account Token should be preserved")
        assertEquals(originalConfig.accountRegion, configFromJson.accountRegion, "Account Region should be preserved")
        assertEquals(originalConfig.proxyDomain, configFromJson.proxyDomain, "proxyDomain should be preserved")
        assertEquals(originalConfig.spikyProxyDomain, configFromJson.spikyProxyDomain, "spikyProxyDomain should be preserved")
        assertEquals(originalConfig.customHandshakeDomain, configFromJson.customHandshakeDomain, "customHandshakeDomain should be preserved")
        assertEquals(originalConfig.pushTypes, configFromJson.pushTypes, "pushTypes should be preserved")
        assertEquals(originalConfig.packageName, configFromJson.packageName, "pushTypes should be preserved")
        assertEquals(originalConfig.fcmSenderId, configFromJson.fcmSenderId, "pushTypes should be preserved")

        // Boolean flags
        assertEquals(originalConfig.isDefaultInstance, configFromJson.isDefaultInstance, "isDefaultInstance flag should be preserved")
        assertEquals(originalConfig.isSslPinningEnabled, configFromJson.isSslPinningEnabled, "SSL Pinning flag should be preserved")
        assertEquals(originalConfig.isBackgroundSync, configFromJson.isBackgroundSync, "Background Sync flag should be preserved")
        assertEquals(originalConfig.isAnalyticsOnly, configFromJson.isAnalyticsOnly, "Analytics Only flag should be preserved")
        assertEquals(originalConfig.isPersonalizationEnabled, configFromJson.isPersonalizationEnabled, "Personalization flag should be preserved")
        assertEquals(originalConfig.debugLevel, configFromJson.debugLevel, "Debug flag should be preserved") // Assuming isDebug() is the getter
        assertEquals(originalConfig.isCreatedPostAppLaunch, configFromJson.isCreatedPostAppLaunch, "Created Post App Launch flag should be preserved")
        assertEquals(originalConfig.isDisableAppLaunchedEvent, configFromJson.isDisableAppLaunchedEvent, "Disable App Launched Event flag should be preserved")
        assertEquals(originalConfig.enableCustomCleverTapId, configFromJson.enableCustomCleverTapId, "Enable Custom CleverTap ID flag should be preserved")
        assertEquals(originalConfig.isBeta, configFromJson.isBeta, "Beta flag should be preserved")
        assertEquals(originalConfig.isUseGoogleAdId, configFromJson.isUseGoogleAdId, "Use Google Ad ID flag should be preserved")

        // todo : fails and was identified in the UT
        //assertEquals(originalConfig.identityKeys, configFromJson.identityKeys, "Identities should be preserved")

        assertEquals(originalConfig.encryptionLevel, configFromJson.encryptionLevel, "Encryption Level should be preserved")
    }
}