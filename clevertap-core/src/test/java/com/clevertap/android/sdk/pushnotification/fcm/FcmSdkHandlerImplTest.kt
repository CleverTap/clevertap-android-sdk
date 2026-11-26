package com.clevertap.android.sdk.pushnotification.fcm

import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_SENDER_ID
import com.clevertap.android.sdk.utils.PackageUtils
import com.clevertap.android.shared.test.BaseTestCase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FcmSdkHandlerImplTest : BaseTestCase() {

    private lateinit var handler: FcmSdkHandlerImpl
    private lateinit var listener: CTPushProviderListener
    private lateinit var manifestInfo: ManifestInfo

    override fun setUp() {
        super.setUp()
        listener = mockk(relaxed = true)
        handler = FcmSdkHandlerImpl(listener, application, cleverTapInstanceConfig)
        manifestInfo = mockk(relaxed = true)
        handler.manifestInfo = manifestInfo
    }

    @Test
    fun isAvailable_Unavailable_PlayServices_Returns_False() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayServicesAvailable(application) } returns false
            assertFalse(handler.isAvailable)
        }
    }

    @Test
    fun isAvailable_InValid_Manifest_Valid_Config_Json_Returns_True() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayServicesAvailable(application) } returns true
            every { manifestInfo.fcmSenderId } returns null
            val app = mockk<FirebaseApp>(relaxed = true)

            mockkStatic(FirebaseApp::class) {
                every { FirebaseApp.getInstance() } returns app
                val options = mockk<FirebaseOptions>(relaxed = true)
                every { app.options } returns options
                every { options.gcmSenderId } returns FCM_SENDER_ID
                assertTrue(handler.isAvailable)
            }
        }
    }

    @Test
    fun isAvailable_InValid_Manifest_InValid_Config_Json_Returns_False() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayServicesAvailable(application) } returns true
            every { manifestInfo.fcmSenderId } returns null
            val app = mockk<FirebaseApp>(relaxed = true)

            mockkStatic(FirebaseApp::class) {
                every { FirebaseApp.getInstance() } returns app
                val options = mockk<FirebaseOptions>(relaxed = true)
                every { app.options } returns options
                every { options.gcmSenderId } returns null
                assertFalse(handler.isAvailable)
            }
        }
    }

    @Test
    fun isAvailable_Exception_Returns_False() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayServicesAvailable(application) } throws RuntimeException(
                "Something Went Wrong"
            )
            assertFalse(handler.isAvailable)
        }
    }

    @Test
    fun isSupported_Returns_True() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayStoreAvailable(application) } returns true
            assertTrue(handler.isSupported)
        }
    }

    @Test
    fun isSupported_Returns_False() {
        mockkObject(PackageUtils) {
            every { PackageUtils.isGooglePlayStoreAvailable(application) } returns false
            assertFalse(handler.isSupported)
        }
    }

    @Test
    fun testGetPushType() {
        assertEquals(handler.pushType, FCM)
    }

    @Test
    fun getSenderId_Invalid_Manifest_Valid_Config_Json() {
        every { manifestInfo.fcmSenderId } returns null
        mockkStatic(FirebaseApp::class) {
            val app = mockk<FirebaseApp>(relaxed = true)
            val options = mockk<FirebaseOptions>(relaxed = true)
            every { FirebaseApp.getInstance() } returns app
            every { app.options } returns options
            every { options.gcmSenderId } returns FCM_SENDER_ID
            assertEquals(handler.senderId, FCM_SENDER_ID)
        }
    }

    @Test
    fun getSenderId_Invalid_Manifest_InValid_Config_Json() {
        every { manifestInfo.fcmSenderId } returns null
        mockkStatic(FirebaseApp::class) {
            val app = mockk<FirebaseApp>(relaxed = true)
            val options = mockk<FirebaseOptions>(relaxed = true)
            every { FirebaseApp.getInstance() } returns app
            every { app.options } returns options
            every { options.gcmSenderId } returns null
            assertNull(handler.senderId)
        }
    }

    @Test
    fun testRequestToken_Exception_Null_Token() {
        mockkStatic(FirebaseMessaging::class) {
            every { FirebaseMessaging.getInstance() } throws RuntimeException("Something Went wrong")
            handler.requestToken()
            verify(exactly = 1) { listener.onNewToken(null, FCM) }
        }
    }
}
