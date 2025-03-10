package com.clevertap.android.sdk.pushnotification

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class PushTypeTest {

    @Test
    fun `fromJSONObject should correctly parse a valid JSONObject`() {
        // Arrange
        val jsonObject = JSONObject().apply {
            put("ctProviderClassName", "TestProvider")
            put("messagingSDKClassName", "TestMessagingSDK")
            put("tokenPrefKey", "TestTokenPrefKey")
            put("type", "TestType")
        }

        // Act
        val pushType = PushType.fromJSONObject(jsonObject)

        assertNotNull(pushType)
        // Assert
        assertEquals("TestProvider", pushType.ctProviderClassName)
        assertEquals("TestMessagingSDK", pushType.messagingSDKClassName)
        assertEquals("TestTokenPrefKey", pushType.tokenPrefKey)
        assertEquals("TestType", pushType.type)
    }

    @Test
    fun `fromJSONObject should return null if JSONObject is missing a key`() {
        // Arrange
        val jsonObject = JSONObject().apply {
            put("ctProviderClassName", "TestProvider")
            // Missing "messagingSDKClassName"
            put("tokenPrefKey", "TestTokenPrefKey")
            put("type", "TestType")
        }

        // Act
        val pushType = PushType.fromJSONObject(jsonObject)

        // Assert
        assertNull(pushType)
    }

    @Test
    fun `fromJSONObject should return null if JSONObject is empty`() {
        // Arrange
        val jsonObject = JSONObject()

        // Act
        val pushType = PushType.fromJSONObject(jsonObject)

        // Assert
        assertNull(pushType)
    }

    @Test
    fun `fromJSONObject should return null if JSONObject is null`() {
        // Arrange
        val jsonObject: JSONObject? = null

        // Act
        val pushType = PushType.fromJSONObject(jsonObject)

        // Assert
        assertNull(pushType)
    }

    @Test
    fun `test toJSONObject with valid data`() {
        // Arrange
        val type = "FCM"
        val prefKey = "fcm_token"
        val className = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider"
        val messagingSDKClassName = "com.google.firebase.messaging.FirebaseMessaging"
        val pushType = PushType(type, prefKey, className, messagingSDKClassName)

        // Act
        val jsonObject = pushType.toJSONObject()

        assertNotNull(jsonObject)
        // Assert
        jsonObject.run {
            assertEquals(className, getString("ctProviderClassName"))
            assertEquals(messagingSDKClassName, getString("messagingSDKClassName"))
            assertEquals(prefKey, getString("tokenPrefKey"))
            assertEquals(type, getString("type"))
        }
    }
}