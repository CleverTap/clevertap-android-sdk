package com.clevertap.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.pushnotification.PushType
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

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

}