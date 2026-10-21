package com.clevertap.android.sdk

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.interfaces.CustomSdkVersionProvider
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationHandlerSdkVersionTest : BaseTestCase() {

    private open class PlainHandler : NotificationHandler {
        override fun onMessageReceived(applicationContext: Context, message: Bundle, pushType: String) = true
        override fun onNewToken(applicationContext: Context, token: String, pushType: PushType) = true
    }

    private class VersionedHandler : PlainHandler(), CustomSdkVersionProvider {
        override fun getCustomSdkName() = "TestModule"
        override fun getCustomSdkVersion() = 10203
    }

    private class ThrowingHandler : PlainHandler(), CustomSdkVersionProvider {
        override fun getCustomSdkName(): String = throw IllegalStateException("boom")
        override fun getCustomSdkVersion() = 1
    }

    @Before
    override fun setUp() {
        super.setUp()
        CleverTapAPI.setInstances(null)
        CleverTapAPI.setNotificationHandler(null)
    }

    @After
    fun tearDown() {
        CleverTapAPI.setNotificationHandler(null)
        CleverTapAPI.setInstances(null)
    }

    @Test
    fun test_setNotificationHandler_whenInstancesExist_registersVersionOnEach() {
        val first = mockk<CleverTapAPI>(relaxed = true)
        val second = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("acc-1" to first, "acc-2" to second))

        CleverTapAPI.setNotificationHandler(VersionedHandler())

        verify(exactly = 1) { first.setCustomSdkVersion("TestModule", 10203) }
        verify(exactly = 1) { second.setCustomSdkVersion("TestModule", 10203) }
    }

    @Test
    fun test_setNotificationHandler_whenNoInstancesYet_doesNotCrash() {
        CleverTapAPI.setNotificationHandler(VersionedHandler())
    }

    @Test
    fun test_registerNotificationHandlerSdkVersion_whenHandlerSetBeforeInstance_registersOnNewInstance() {
        CleverTapAPI.setNotificationHandler(VersionedHandler())
        val created = mockk<CleverTapAPI>(relaxed = true)

        // instanceWithConfig calls this right after storing a newly created instance
        CleverTapAPI.registerNotificationHandlerSdkVersion(created)

        verify(exactly = 1) { created.setCustomSdkVersion("TestModule", 10203) }
    }

    @Test
    fun test_registerNotificationHandlerSdkVersion_whenHandlerHasNoVersion_doesNothing() {
        val instance = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("acc-1" to instance))

        CleverTapAPI.setNotificationHandler(PlainHandler())
        CleverTapAPI.registerNotificationHandlerSdkVersion(instance)

        verify(exactly = 0) { instance.setCustomSdkVersion(any(), any()) }
    }

    @Test
    fun test_registerNotificationHandlerSdkVersion_whenNoHandlerSet_doesNothing() {
        val instance = mockk<CleverTapAPI>(relaxed = true)

        CleverTapAPI.registerNotificationHandlerSdkVersion(instance)

        verify(exactly = 0) { instance.setCustomSdkVersion(any(), any()) }
    }

    @Test
    fun test_setNotificationHandler_whenProviderThrows_doesNotCrash() {
        val instance = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("acc-1" to instance))

        CleverTapAPI.setNotificationHandler(ThrowingHandler())

        verify(exactly = 0) { instance.setCustomSdkVersion(any(), any()) }
    }

    @Test
    fun test_setNotificationHandler_whenSetCustomSdkVersionThrows_doesNotCrash() {
        val instance = mockk<CleverTapAPI>(relaxed = true)
        every { instance.setCustomSdkVersion(any(), any()) } throws NoSuchMethodError("setCustomSdkVersion")
        CleverTapAPI.setInstances(hashMapOf("acc-1" to instance))

        CleverTapAPI.setNotificationHandler(VersionedHandler())

        verify(exactly = 1) { instance.setCustomSdkVersion("TestModule", 10203) }
    }
}
