package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.*
import org.junit.Test
import org.junit.jupiter.api.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class FcmNotificationBundleManipulationTest : BaseTestCase() {

    private lateinit var messageBundle: Bundle

    @Mock
    private lateinit var remoteMessage: RemoteMessage
    private lateinit var notificationManipulation: FcmNotificationBundleManipulation

    @BeforeEach
    override fun setUp() {
        MockitoAnnotations.initMocks(this)
        messageBundle = Bundle()
        notificationManipulation = FcmNotificationBundleManipulation(messageBundle)
    }

    @Test
    fun addPriority_withDifferentOriginalPriority_shouldAddPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_HIGH
        val originalPriority = RemoteMessage.PRIORITY_NORMAL
        val strPriority = Constants.PRIORITY_HIGH

        `when`(remoteMessage.originalPriority).thenReturn(originalPriority)
        `when`(remoteMessage.priority).thenReturn(priority)

        assertEquals(null, messageBundle.getString(Constants.WZRK_PN_PRT))

        val actualBundle = notificationManipulation.addPriority(remoteMessage).build()

        assertEquals(messageBundle, actualBundle)

        assertEquals(strPriority, messageBundle.getString(Constants.WZRK_PN_PRT))
    }

    @Test
    fun addPriority_withSameOriginalPriority_shouldNotAddPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_NORMAL
        val originalPriority = RemoteMessage.PRIORITY_NORMAL

        `when`(remoteMessage.originalPriority).thenReturn(originalPriority)
        `when`(remoteMessage.priority).thenReturn(priority)

        val actualBundle = notificationManipulation.addPriority(remoteMessage).build()

        assertEquals(messageBundle, actualBundle)
        assertEquals(null, messageBundle.getString(Constants.WZRK_PN_PRT))
    }

    @Test
    fun build_shouldReturnSameBundleInstance() {
        val result = notificationManipulation.build()

        assertEquals(messageBundle, result)
    }
}