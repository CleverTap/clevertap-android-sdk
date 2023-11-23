package com.clevertap.android.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTXtensionsTest : BaseTestCase() {

    @Test
    fun test_when_sdkInt_and_targetSdkVersion_is_33_and_input_is_32_should_return_true() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertTrue { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_33_and_targetSdkVersion_is_32_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 32

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_32_and_targetSdkVersion_is_33_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 32)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_30_and_targetSdkVersion_is_30_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 30)

        application.applicationContext.applicationInfo.targetSdkVersion = 30

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreEnabled_and_channelImportanceIsNone_should_return_false() {

        configureTestNotificationChannel(NotificationManager.IMPORTANCE_NONE, true, 30)
        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreEnabled_and_channelImportanceIsMAX_should_return_true() {

        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 30)
        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertTrue { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreDisabled_and_channelImportanceIsNone_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_NONE, false, 30)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreDisabled_and_channelImportanceIsMAX_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, false, 30)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_25_and_notificationsAreDisabled_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, false, 25)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_25_and_notificationsAreEnabled_should_return_true() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 25)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertTrue { actual }
    }

    //given = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_registered_then_return_its_channelID() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 30)
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val actual = nm.getOrCreateChannel("BlockedBRTesting",application)
        assertEquals("BlockedBRTesting",actual)
    }

    //given = null | manifest = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_is_null_and_manifestChannel_is_registered_then_return_manifestChannel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_MAX, true, 30,
                "ManifestChannelId", "ManifestChannelName"
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals("ManifestChannelId", actual)
        }
    }

    //given = null | manifest = null | default = not registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_null_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = null | default = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_is_null_and_manifestChannel_is_null_and_fallback_channel_exists_then_return_fallback_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = not registered | manifest = null | default = not registered
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_available_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }
    //given = not registered | manifest = not registered | default
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = not registered | manifest = null | default = registered
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_available_and_fallback_channel_exists_then_return_fallback_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = not registered | default = not registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_not_registered_and_default_not_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("NonRegisteredManifestChannelId")

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = not registered | default = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_not_registered_and_default_is_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("NonRegisteredManifestChannelId")

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }


    @Test
    fun test_getOrCreateChannel_when_getNotificationChannel_throws_exception_return_null() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = mock(NotificationManager::class.java)
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_MAX, true, 30,
                "ManifestChannelId", "ManifestChannelName"
            )

            // Throw an exception from the `getNotificationChannel()` method.
            `when`(nm.getNotificationChannel("ManifestChannelId")).thenThrow(RuntimeException())

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(null, actual)

        }
    }

    @Test
    fun `test isInvalidIndex with null JSONArray`() {
        val jsonArray: JSONArray? = null
        assertTrue(jsonArray.isInvalidIndex(0))
    }

    @Test
    fun `test isInvalidIndex with empty JSONArray`() {
        val jsonArray = JSONArray()
        assertTrue(jsonArray.isInvalidIndex(0))
    }

    @Test
    fun `test isInvalidIndex with valid index`() {
        val jsonArray = JSONArray("[1, 2, 3]")
        assertFalse(jsonArray.isInvalidIndex(0))
        assertFalse(jsonArray.isInvalidIndex(1))
        assertFalse(jsonArray.isInvalidIndex(2))
    }

    @Test
    fun `test isInvalidIndex with invalid index at right and left boundary`() {
        val jsonArray = JSONArray("[1, 2, 3]")
        assertTrue(jsonArray.isInvalidIndex(3))
        assertTrue(jsonArray.isInvalidIndex(-1))
    }

    @Test
    fun `test hasData with empty SharedPreferences`() {
        val sharedPreferences = application.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Ensure the SharedPreferences is empty
        assertTrue(sharedPreferences.all.isEmpty())

        // Test the hasData function
        assertFalse(sharedPreferences.hasData())
    }

    @Test
    fun `test hasData with non-empty SharedPreferences`() {
        val sharedPreferences = application.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Add some data to SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("key1", "value1")
        editor.putString("key2", "value2")
        editor.apply()

        // Ensure the SharedPreferences is not empty
        assertTrue(sharedPreferences.all.isNotEmpty())

        // Test the hasData function
        assertTrue(sharedPreferences.hasData())
    }

    private fun configureTestNotificationChannel(
        importance: Int, areChannelsEnabled: Boolean, SDK_INT: Int, channelID: String = "BlockedBRTesting",
        channelName: String = "BlockedBRTesting",
    ) {
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(nm)
        shadowNotificationManager.setNotificationsEnabled(areChannelsEnabled)
        val notificationChannel = NotificationChannel(
            channelID,
            channelName,
            importance
        )
        notificationChannel.description = "channelDescription"
        nm.createNotificationChannel(notificationChannel)
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", SDK_INT)
    }
}