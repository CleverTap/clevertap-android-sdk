package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FcmNotificationBundleManipulationTest : BaseTestCase() {

    private lateinit var messageBundle: Bundle
    private lateinit var remoteMessage: RemoteMessage
    private lateinit var notificationManipulation: FcmNotificationBundleManipulation

    override fun setUp() {
        messageBundle = Bundle()
        remoteMessage = mockk(relaxed = true)
        notificationManipulation = FcmNotificationBundleManipulation(messageBundle)
    }

    @Test
    fun addPriority_withDifferentOriginalPriority_shouldAddHighPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_HIGH
        val originalPriority = RemoteMessage.PRIORITY_NORMAL
        val strPriority = Constants.PRIORITY_HIGH

        every { remoteMessage.originalPriority } returns originalPriority
        every { remoteMessage.priority } returns priority

        assertEquals(null, messageBundle.getString(Constants.WZRK_PN_PRT))

        val actualBundle = notificationManipulation.addPriority(remoteMessage).build()

        assertEquals(messageBundle, actualBundle)

        assertEquals(strPriority, messageBundle.getString(Constants.WZRK_PN_PRT))
    }

    @Test
    fun addPriority_withDifferentOriginalPriority_shouldAddNormalPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_NORMAL
        val originalPriority = RemoteMessage.PRIORITY_HIGH
        val strPriority = Constants.PRIORITY_NORMAL

        every { remoteMessage.originalPriority } returns originalPriority
        every { remoteMessage.priority } returns priority

        assertEquals(null, messageBundle.getString(Constants.WZRK_PN_PRT))

        val actualBundle = notificationManipulation.addPriority(remoteMessage).build()

        assertEquals(messageBundle, actualBundle)

        assertEquals(strPriority, messageBundle.getString(Constants.WZRK_PN_PRT))
    }
    @Test
    fun addPriority_withDifferentOriginalPriority_shouldAddUnknownPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_UNKNOWN
        val originalPriority = RemoteMessage.PRIORITY_HIGH
        val strPriority = Constants.PRIORITY_UNKNOWN

        every { remoteMessage.originalPriority } returns originalPriority
        every { remoteMessage.priority } returns priority

        assertEquals(null, messageBundle.getString(Constants.WZRK_PN_PRT))

        val actualBundle = notificationManipulation.addPriority(remoteMessage).build()

        assertEquals(messageBundle, actualBundle)

        assertEquals(strPriority, messageBundle.getString(Constants.WZRK_PN_PRT))
    }

    @Test
    fun addPriority_withSameOriginalPriority_shouldNotAddPriorityToBundle() {
        val priority = RemoteMessage.PRIORITY_NORMAL
        val originalPriority = RemoteMessage.PRIORITY_NORMAL

        every { remoteMessage.originalPriority } returns originalPriority
        every { remoteMessage.priority } returns priority

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
