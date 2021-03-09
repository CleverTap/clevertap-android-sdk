package com.clevertap.android.sdk.pushnotification.fcm

import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_SENDER_ID
import com.clevertap.android.sdk.utils.PackageUtils
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.iid.FirebaseInstanceId
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class FcmSdkHandlerImplTest : BaseTestCase() {

    private lateinit var handler: FcmSdkHandlerImpl
    private lateinit var listener: CTPushProviderListener
    private lateinit var manifestInfo: ManifestInfo

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        listener = mock(CTPushProviderListener::class.java)
        handler = FcmSdkHandlerImpl(listener, application, cleverTapInstanceConfig)
        manifestInfo = mock(ManifestInfo::class.java)
        handler.setManifestInfo(manifestInfo)
    }

    @Test
    fun isAvailable_Unavailable_PlayServices_Returns_False() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayServicesAvailable(application)).thenReturn(false)
            Assert.assertFalse(handler.isAvailable)
        }
    }

    @Test
    fun isAvailable_Valid_Manifest_Returns_True() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayServicesAvailable(application)).thenReturn(true)
            `when`(manifestInfo.fcmSenderId).thenReturn(FCM_SENDER_ID)
            Assert.assertTrue(handler.isAvailable)
        }
    }

    @Test
    fun isAvailable_InValid_Manifest_Valid_Config_Json_Returns_True() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayServicesAvailable(application)).thenReturn(true)
            `when`(manifestInfo.fcmSenderId).thenReturn(null)
            val app = mock(FirebaseApp::class.java)

            mockStatic(FirebaseApp::class.java).use {
                `when`(FirebaseApp.getInstance()).thenReturn(app)
                val options = mock(FirebaseOptions::class.java)
                `when`(app.options).thenReturn(options)
                `when`(options.gcmSenderId).thenReturn(FCM_SENDER_ID)
                Assert.assertTrue(handler.isAvailable)
            }
        }
    }

    @Test
    fun isAvailable_InValid_Manifest_InValid_Config_Json_Returns_False() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayServicesAvailable(application)).thenReturn(true)
            `when`(manifestInfo.fcmSenderId).thenReturn(null)
            val app = mock(FirebaseApp::class.java)

            mockStatic(FirebaseApp::class.java).use {
                `when`(FirebaseApp.getInstance()).thenReturn(app)
                val options = mock(FirebaseOptions::class.java)
                `when`(app.options).thenReturn(options)
                `when`(options.gcmSenderId).thenReturn(null)
                Assert.assertFalse(handler.isAvailable)
            }
        }
    }

    @Test
    fun isAvailable_Exception_Returns_False() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayServicesAvailable(application)).thenThrow(RuntimeException("Something Went Wrong"))
            Assert.assertFalse(handler.isAvailable)
        }
    }

    @Test
    fun isSupported_Returns_True() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayStoreAvailable(application)).thenReturn(true)
            Assert.assertTrue(handler.isSupported)
        }
    }

    @Test
    fun isSupported_Returns_False() {
        mockStatic(PackageUtils::class.java).use {
            `when`(PackageUtils.isGooglePlayStoreAvailable(application)).thenReturn(false)
            Assert.assertFalse(handler.isSupported)
        }
    }

    @Test
    fun testGetPushType() {
        Assert.assertEquals(handler.pushType, FCM)
    }

    @Test
    fun testGetFCMSenderID() {
        handler.fcmSenderID
        verify(manifestInfo, times(1)).fcmSenderId
    }

    @Test
    fun getSenderId_Valid_Manifest() {
        `when`(manifestInfo.fcmSenderId).thenReturn(FCM_SENDER_ID)
        Assert.assertEquals(handler.senderId, FCM_SENDER_ID)
    }

    @Test
    fun getSenderId_Invalid_Manifest_Valid_Config_Json() {
        `when`(manifestInfo.fcmSenderId).thenReturn(null)
        mockStatic(FirebaseApp::class.java).use {
            val app = mock(FirebaseApp::class.java)
            val options = mock(FirebaseOptions::class.java)
            `when`(FirebaseApp.getInstance()).thenReturn(app)
            `when`(app.options).thenReturn(options)
            `when`(options.gcmSenderId).thenReturn(FCM_SENDER_ID)
            Assert.assertEquals(handler.senderId, FCM_SENDER_ID)
        }
    }

    @Test
    fun getSenderId_Invalid_Manifest_InValid_Config_Json() {
        `when`(manifestInfo.fcmSenderId).thenReturn(null)
        mockStatic(FirebaseApp::class.java).use {
            val app = mock(FirebaseApp::class.java)
            val options = mock(FirebaseOptions::class.java)
            `when`(FirebaseApp.getInstance()).thenReturn(app)
            `when`(app.options).thenReturn(options)
            `when`(options.gcmSenderId).thenReturn(null)
            Assert.assertNull(handler.senderId)
        }
    }

    @Test
    fun testRequestToken_Exception_Null_Token() {
        mockStatic(FirebaseInstanceId::class.java).use {
            `when`(FirebaseInstanceId.getInstance()).thenThrow(RuntimeException("Something Went wrong"))
            handler.requestToken()
            verify(listener, times(1)).onNewToken(null, FCM)
        }
    }
}