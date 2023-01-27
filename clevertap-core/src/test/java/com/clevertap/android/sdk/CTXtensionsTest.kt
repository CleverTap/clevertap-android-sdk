package com.clevertap.android.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
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

    private fun configureTestNotificationChannel(importance: Int, areChannelsEnabled: Boolean, SDK_INT: Int) {
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(nm)
        shadowNotificationManager.setNotificationsEnabled(areChannelsEnabled)
        val notificationChannel = NotificationChannel(
            "BlockedBRTesting",
            "BlockedBRTesting",
            importance
        )
        notificationChannel.description = "channelDescription"
        nm.createNotificationChannel(notificationChannel)
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", SDK_INT)
    }
}